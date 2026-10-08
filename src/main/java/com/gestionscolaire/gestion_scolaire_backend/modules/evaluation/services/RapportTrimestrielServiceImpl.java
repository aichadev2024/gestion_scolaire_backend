package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories.EtablissementRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto.RapportTrimestrielRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto.RapportTrimestrielResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto.RapportTrimestrielResponse.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.Note;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.RapportTrimestriel;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.repositories.NoteRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.repositories.RapportTrimestrielRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.Paiement;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.repositories.PaiementRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.ClasseMatiere;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.FraisScolarite;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.IncidentDiscipline;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Presence;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseMatiereRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EleveRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.FraisScolariteRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.IncidentDisciplineRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.PresenceRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class RapportTrimestrielServiceImpl implements RapportTrimestrielService {

    /** En dessous de cette moyenne, un élève est signalé « en difficulté ». */
    static final double SEUIL_DIFFICULTE = 8.0;
    static final double SEUIL_REUSSITE = 10.0;
    private static final int TAILLE_CLASSEMENT = 5;
    private static final int TAILLE_DIFFICULTE = 10;

    private final TenantGuard tenantGuard;
    private final EtablissementRepository etablissementRepository;
    private final ClasseRepository classeRepository;
    private final ClasseMatiereRepository classeMatiereRepository;
    private final EleveRepository eleveRepository;
    private final NoteRepository noteRepository;
    private final PresenceRepository presenceRepository;
    private final IncidentDisciplineRepository incidentRepository;
    private final RapportTrimestrielRepository rapportRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final FraisScolariteRepository fraisRepository;
    private final PaiementRepository paiementRepository;

    public RapportTrimestrielServiceImpl(TenantGuard tenantGuard, EtablissementRepository etablissementRepository,
                                         ClasseRepository classeRepository, ClasseMatiereRepository classeMatiereRepository,
                                         EleveRepository eleveRepository, NoteRepository noteRepository,
                                         PresenceRepository presenceRepository, IncidentDisciplineRepository incidentRepository,
                                         RapportTrimestrielRepository rapportRepository, UtilisateurRepository utilisateurRepository,
                                         FraisScolariteRepository fraisRepository, PaiementRepository paiementRepository) {
        this.tenantGuard = tenantGuard;
        this.etablissementRepository = etablissementRepository;
        this.classeRepository = classeRepository;
        this.classeMatiereRepository = classeMatiereRepository;
        this.eleveRepository = eleveRepository;
        this.noteRepository = noteRepository;
        this.presenceRepository = presenceRepository;
        this.incidentRepository = incidentRepository;
        this.rapportRepository = rapportRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.fraisRepository = fraisRepository;
        this.paiementRepository = paiementRepository;
    }

    private static Integer niveauDe(Classe c) {
        return c.getNiveau() != null ? c.getNiveau().getId() : null;
    }

    private Classe classeAutorisee(Long classeId) {
        Classe classe = tenantGuard.requireSameTenant(classeRepository.findById(classeId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable")));
        tenantGuard.requireSameNiveau(classe, RapportTrimestrielServiceImpl::niveauDe);
        return classe;
    }

    private Optional<RapportTrimestriel> rapportEnregistre(Long etablissementId, Long classeId, String periode, String annee) {
        return classeId == null
                ? rapportRepository.findByEtablissementIdAndClasseIsNullAndPeriodeAndAnneeScolaire(etablissementId, periode, annee)
                : rapportRepository.findByEtablissementIdAndClasseIdAndPeriodeAndAnneeScolaire(etablissementId, classeId, periode, annee);
    }

    @Override
    @Transactional(readOnly = true)
    public RapportTrimestrielResponse generer(Long classeId, String periode, String anneeScolaire, LocalDate debut, LocalDate fin) {
        Long etablissementId = tenantGuard.requireEtablissementId();
        String per = normaliserPeriode(periode);
        Etablissement etab = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResourceNotFoundException("Établissement introuvable"));

        Classe classeChoisie = classeId != null ? classeAutorisee(classeId) : null;
        List<Classe> classes = classeChoisie != null
                ? List.of(classeChoisie)
                : tenantGuard.filterSameNiveau(classeRepository.findByEtablissementId(etablissementId), RapportTrimestrielServiceImpl::niveauDe);

        Optional<RapportTrimestriel> enregistre = rapportEnregistre(etablissementId, classeId, per, anneeScolaire);
        LocalDate dateDebut = debut != null ? debut : enregistre.map(RapportTrimestriel::getDateDebut).orElse(null);
        LocalDate dateFin = fin != null ? fin : enregistre.map(RapportTrimestriel::getDateFin).orElse(null);
        if (dateDebut == null || dateFin == null) {
            LocalDate[] defaut = periodeParDefaut(per, anneeScolaire);
            if (dateDebut == null) dateDebut = defaut[0];
            if (dateFin == null) dateFin = defaut[1];
        }
        if (dateFin.isBefore(dateDebut)) {
            throw new BadRequestException("La date de fin doit suivre la date de début.");
        }

        // Élèves actifs des classes concernées
        Set<Long> idsClasses = classes.stream().map(Classe::getId).collect(Collectors.toSet());
        List<Eleve> eleves = eleveRepository.findByEtablissementId(etablissementId).stream()
                .filter(e -> e.getClasse() != null && idsClasses.contains(e.getClasse().getId()))
                .filter(e -> e.getStatut() == null || "ACTIF".equalsIgnoreCase(e.getStatut()))
                .toList();
        Set<Long> idsEleves = eleves.stream().map(Eleve::getId).collect(Collectors.toSet());

        // Notes de la période, rangées par élève puis par matière de classe
        Map<Long, Map<Long, List<Double>>> notes = new HashMap<>();
        if (!idsEleves.isEmpty()) {
            for (Note n : noteRepository.findByEleveIdInAndPeriodeIgnoreCase(idsEleves, per)) {
                if (n.getValeur() == null) continue;
                notes.computeIfAbsent(n.getEleve().getId(), k -> new HashMap<>())
                        .computeIfAbsent(n.getClasseMatiere().getId(), k -> new ArrayList<>())
                        .add(n.getValeur());
            }
        }

        Map<Long, List<ClasseMatiere>> matieresParClasse = new HashMap<>();
        for (Classe c : classes) {
            matieresParClasse.put(c.getId(), classeMatiereRepository.findByClasseId(c.getId()));
        }

        // Moyenne générale de chaque élève évalué — même règle que le bulletin (matière sans note = 0)
        Map<Long, Double> moyennes = new HashMap<>();
        for (Eleve e : eleves) {
            Map<Long, List<Double>> notesEleve = notes.get(e.getId());
            if (notesEleve == null) continue;
            double points = 0, coefs = 0;
            for (ClasseMatiere cm : matieresParClasse.getOrDefault(e.getClasse().getId(), List.of())) {
                double coef = cm.getCoefficient() != null ? cm.getCoefficient() : 1.0;
                points += moyenneDe(notesEleve.get(cm.getId())).orElse(0.0) * coef;
                coefs += coef;
            }
            if (coefs > 0) moyennes.put(e.getId(), points / coefs);
        }

        // Présences et discipline sur la période
        Map<Long, Long> classeDeLEleve = eleves.stream().collect(Collectors.toMap(Eleve::getId, e -> e.getClasse().getId()));
        List<Presence> presences = presenceRepository.findByEtablissementIdAndDateBetween(etablissementId, dateDebut, dateFin)
                .stream().filter(p -> p.getEleve() != null && idsEleves.contains(p.getEleve().getId())).toList();
        List<IncidentDiscipline> incidents = incidentRepository.findByEtablissementIdAndDateBetween(etablissementId, dateDebut, dateFin)
                .stream().filter(i -> i.getClasse() != null && idsClasses.contains(i.getClasse().getId())).toList();

        List<LigneMatiere> lignesMatieres = classeChoisie != null
                ? lignesMatieres(eleves, notes, matieresParClasse.get(classeChoisie.getId()))
                : List.of();

        // Finances : réservées à la direction (le secrétariat n'a pas accès aux paiements)
        Map<Long, double[]> financesParClasse = new HashMap<>();
        Finances finances = peutVoirFinances()
                ? calculerFinances(etab, classes, eleves, dateDebut, dateFin, financesParClasse)
                : null;

        List<LigneClasse> lignesClasses = classeChoisie != null ? List.of() : classes.stream()
                .sorted(Comparator.comparing(Classe::getNom, String.CASE_INSENSITIVE_ORDER))
                .map(c -> {
                    List<Eleve> deLaClasse = eleves.stream().filter(e -> e.getClasse().getId().equals(c.getId())).toList();
                    List<Double> mm = deLaClasse.stream().map(e -> moyennes.get(e.getId())).filter(Objects::nonNull).toList();
                    return new LigneClasse(c.getId(), c.getNom(), deLaClasse.size(), mm.size(), moyenneOuNull(mm),
                            tauxReussite(mm),
                            tauxPresence(presences.stream().filter(p -> c.getId().equals(classeDeLEleve.get(p.getEleve().getId()))).toList()),
                            finances == null ? null : tauxRecouvrement(financesParClasse.get(c.getId())));
                })
                .toList();

        List<EleveClassement> classement = eleves.stream()
                .filter(e -> moyennes.containsKey(e.getId()))
                .map(e -> new EleveClassement(nomComplet(e.getProfil()), e.getClasse().getNom(), r2(moyennes.get(e.getId()))))
                .toList();
        List<EleveClassement> meilleurs = classement.stream()
                .sorted(Comparator.comparingDouble(EleveClassement::moyenne).reversed())
                .limit(TAILLE_CLASSEMENT).toList();
        List<EleveClassement> enDifficulte = classement.stream()
                .filter(c -> c.moyenne() < SEUIL_DIFFICULTE)
                .sorted(Comparator.comparingDouble(EleveClassement::moyenne))
                .limit(TAILLE_DIFFICULTE).toList();

        Map<String, Integer> parStatut = new LinkedHashMap<>();
        incidents.forEach(i -> parStatut.merge(i.getStatut(), 1, Integer::sum));

        List<Double> toutes = new ArrayList<>(moyennes.values());
        return new RapportTrimestrielResponse(
                entete(etab),
                classeChoisie != null ? "Classe de " + classeChoisie.getNom() : "Ensemble de l'établissement",
                classeId, per, anneeScolaire, dateDebut, dateFin,
                enregistre.map(RapportTrimestriel::getCommentaire).orElse(null),
                LocalDateTime.now(),
                new Effectifs(eleves.size(),
                        (int) eleves.stream().filter(e -> genre(e, "M")).count(),
                        (int) eleves.stream().filter(e -> genre(e, "F")).count()),
                synthesePresences(presences),
                new Resultats(toutes.size(), eleves.size() - toutes.size(), moyenneOuNull(toutes),
                        toutes.isEmpty() ? null : r2(Collections.min(toutes)),
                        toutes.isEmpty() ? null : r2(Collections.max(toutes)),
                        tauxReussite(toutes), mentions(toutes)),
                lignesMatieres, lignesClasses, meilleurs, enDifficulte,
                new Discipline(incidents.size(), parStatut,
                        (int) incidents.stream().filter(i -> !Boolean.TRUE.equals(i.getEstTraite())).count()),
                finances);
    }

    @Override
    public void enregistrer(RapportTrimestrielRequest request) {
        Long etablissementId = tenantGuard.requireEtablissementId();
        String per = normaliserPeriode(request.getPeriode());
        Classe classe = request.getClasseId() != null ? classeAutorisee(request.getClasseId()) : null;
        if (request.getDateDebut() != null && request.getDateFin() != null
                && request.getDateFin().isBefore(request.getDateDebut())) {
            throw new BadRequestException("La date de fin doit suivre la date de début.");
        }

        RapportTrimestriel rapport = rapportEnregistre(etablissementId, request.getClasseId(), per, request.getAnneeScolaire())
                .orElseGet(() -> RapportTrimestriel.builder()
                        .classe(classe).periode(per).anneeScolaire(request.getAnneeScolaire()).build());
        rapport.setDateDebut(request.getDateDebut());
        rapport.setDateFin(request.getDateFin());
        String commentaire = request.getCommentaire();
        rapport.setCommentaire(commentaire == null || commentaire.isBlank() ? null : commentaire.trim());
        rapport.setModifiePar(utilisateurRepository.findById(SecurityUtils.getCurrentUserId()).orElse(null));
        rapportRepository.save(rapport);
    }

    // ───────────── calculs ─────────────

    private static boolean peutVoirFinances() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getAuthorities().stream().anyMatch(g -> "ROLE_DIRECTEUR".equals(g.getAuthority()));
    }

    /** {@code parClasse} reçoit, par classe, {attendu, encaissé} pour le taux de recouvrement de chaque ligne. */
    private Finances calculerFinances(Etablissement etab, List<Classe> classes, List<Eleve> eleves,
                                      LocalDate debut, LocalDate fin, Map<Long, double[]> parClasse) {
        LocalDate aujourdHui = LocalDate.now();
        Map<Long, List<FraisScolarite>> fraisParClasse = new HashMap<>();
        for (Classe c : classes) fraisParClasse.put(c.getId(), fraisRepository.findByClasseId(c.getId()));

        Map<Long, Double> payeParEleve = new HashMap<>();
        double encaisseSurPeriode = 0;
        if (!eleves.isEmpty()) {
            Set<Long> ids = eleves.stream().map(Eleve::getId).collect(Collectors.toSet());
            for (Paiement p : paiementRepository.findByEleveIdIn(ids)) {
                double m = p.getMontantPaye() != null ? p.getMontantPaye() : 0;
                payeParEleve.merge(p.getEleve().getId(), m, Double::sum);
                LocalDate jour = p.getDatePaiement() != null ? p.getDatePaiement().toLocalDate() : null;
                if (jour != null && !jour.isBefore(debut) && !jour.isAfter(fin)) encaisseSurPeriode += m;
            }
        }

        double attendu = 0, encaisse = 0, arrieres = 0, enRetard = 0;
        int elevesEnRetard = 0;
        for (Eleve e : eleves) {
            List<FraisScolarite> frais = fraisParClasse.getOrDefault(e.getClasse().getId(), List.of());
            double arr = e.getArrieresMontant() != null ? e.getArrieresMontant() : 0;
            double du = frais.stream().mapToDouble(f -> f.getMontant() != null ? f.getMontant() : 0).sum() + arr;
            double paye = payeParEleve.getOrDefault(e.getId(), 0.0);
            // Même règle que la liste des retards : échéance dépassée d'au moins un jour, arriérés compris
            double echu = frais.stream().filter(f -> f.getDateEcheance() != null && f.getDateEcheance().isBefore(aujourdHui))
                    .mapToDouble(f -> f.getMontant() != null ? f.getMontant() : 0).sum() + arr;
            boolean aUneEcheance = frais.stream().anyMatch(f -> f.getDateEcheance() != null && f.getDateEcheance().isBefore(aujourdHui));
            double retard = aUneEcheance ? Math.max(0, echu - paye) : 0;

            attendu += du;
            encaisse += paye;
            arrieres += arr;
            enRetard += retard;
            if (retard > 0.009) elevesEnRetard++;
            double[] cumul = parClasse.computeIfAbsent(e.getClasse().getId(), k -> new double[2]);
            cumul[0] += du;
            cumul[1] += paye;
        }
        String devise = etab.getDevise() != null && !etab.getDevise().isBlank() ? etab.getDevise() : "FCFA";
        return new Finances(devise, r2(attendu), r2(encaisse), r2(Math.max(0, attendu - encaisse)),
                attendu <= 0 ? null : r2(100.0 * Math.min(encaisse, attendu) / attendu),
                r2(arrieres), r2(enRetard), elevesEnRetard, r2(encaisseSurPeriode));
    }

    private static Double tauxRecouvrement(double[] cumul) {
        return cumul == null || cumul[0] <= 0 ? null : r2(100.0 * Math.min(cumul[1], cumul[0]) / cumul[0]);
    }

    private List<LigneMatiere> lignesMatieres(List<Eleve> eleves, Map<Long, Map<Long, List<Double>>> notes,
                                              List<ClasseMatiere> matieres) {
        List<LigneMatiere> lignes = new ArrayList<>();
        for (ClasseMatiere cm : matieres) {
            List<Double> parEleve = new ArrayList<>();
            for (Eleve e : eleves) {
                Map<Long, List<Double>> notesEleve = notes.get(e.getId());
                if (notesEleve == null) continue;
                moyenneDe(notesEleve.get(cm.getId())).ifPresent(parEleve::add);
            }
            lignes.add(new LigneMatiere(
                    cm.getMatiere() != null ? cm.getMatiere().getNom() : "—",
                    cm.getEnseignant() != null ? nomComplet(cm.getEnseignant().getProfil()) : null,
                    cm.getCoefficient() != null ? cm.getCoefficient() : 1.0,
                    moyenneOuNull(parEleve), tauxReussite(parEleve), parEleve.size()));
        }
        lignes.sort(Comparator.comparing(LigneMatiere::matiere, String.CASE_INSENSITIVE_ORDER));
        return lignes;
    }

    private static Presences synthesePresences(List<Presence> presences) {
        int presents = 0, retards = 0, justifiees = 0, nonJustifiees = 0;
        for (Presence p : presences) {
            String s = p.getStatut() == null ? "" : p.getStatut().toUpperCase();
            switch (s) {
                case "PRESENT" -> presents++;
                case "RETARD" -> retards++;
                case "ABSENT" -> { if (Boolean.TRUE.equals(p.getEstJustifie())) justifiees++; else nonJustifiees++; }
                default -> { /* congé : hors calcul */ }
            }
        }
        return new Presences(presents, retards, justifiees, nonJustifiees, tauxPresence(presents, retards, justifiees + nonJustifiees));
    }

    private static Double tauxPresence(List<Presence> presences) {
        Presences p = synthesePresences(presences);
        return p.tauxPresence();
    }

    private static Double tauxPresence(int presents, int retards, int absents) {
        int total = presents + retards + absents;
        return total == 0 ? null : r2(100.0 * (presents + retards) / total);
    }

    private static Map<String, Integer> mentions(List<Double> moyennes) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (String k : List.of("Très bien", "Bien", "Assez bien", "Passable", "Insuffisant")) m.put(k, 0);
        for (double v : moyennes) {
            String k = v >= 16 ? "Très bien" : v >= 14 ? "Bien" : v >= 12 ? "Assez bien" : v >= 10 ? "Passable" : "Insuffisant";
            m.merge(k, 1, Integer::sum);
        }
        return m;
    }

    private static Double tauxReussite(List<Double> moyennes) {
        if (moyennes.isEmpty()) return null;
        return r2(100.0 * moyennes.stream().filter(v -> v >= SEUIL_REUSSITE).count() / moyennes.size());
    }

    private static Double moyenneOuNull(List<Double> valeurs) {
        return valeurs.isEmpty() ? null : r2(valeurs.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }

    private static OptionalDouble moyenneDe(List<Double> valeurs) {
        return valeurs == null || valeurs.isEmpty()
                ? OptionalDouble.empty()
                : valeurs.stream().mapToDouble(Double::doubleValue).average();
    }

    private static double r2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static boolean genre(Eleve e, String code) {
        return e.getProfil() != null && code.equalsIgnoreCase(e.getProfil().getGenre());
    }

    private static String nomComplet(Profil p) {
        return p == null ? "—" : ((p.getNom() == null ? "" : p.getNom().toUpperCase()) + " "
                + (p.getPrenom() == null ? "" : p.getPrenom())).trim();
    }

    private static Entete entete(Etablissement e) {
        return new Entete(e.getNom(), e.getSlogan(), e.getAdresse(), e.getTelephone(), e.getEmailContact(),
                e.getLogoUrl(), e.getRive(), e.getCap());
    }

    private static String normaliserPeriode(String periode) {
        if (periode == null || periode.isBlank()) throw new BadRequestException("La période est obligatoire.");
        return periode.trim().toUpperCase();
    }

    /** Calendrier habituel : T1 oct.–déc., T2 janv.–mars, T3 avr.–juin ; sinon toute l'année scolaire. */
    static LocalDate[] periodeParDefaut(String periode, String anneeScolaire) {
        int debutAnnee;
        try {
            debutAnnee = Integer.parseInt(anneeScolaire.trim().substring(0, 4));
        } catch (RuntimeException e) {
            throw new BadRequestException("Année scolaire invalide (attendu : 2026/2027).");
        }
        return switch (periode) {
            case "TRIMESTRE_1" -> new LocalDate[]{LocalDate.of(debutAnnee, 10, 1), LocalDate.of(debutAnnee, 12, 31)};
            case "TRIMESTRE_2" -> new LocalDate[]{LocalDate.of(debutAnnee + 1, 1, 1), LocalDate.of(debutAnnee + 1, 3, 31)};
            case "TRIMESTRE_3" -> new LocalDate[]{LocalDate.of(debutAnnee + 1, 4, 1), LocalDate.of(debutAnnee + 1, 6, 30)};
            default -> new LocalDate[]{LocalDate.of(debutAnnee, 9, 1), LocalDate.of(debutAnnee + 1, 7, 31)};
        };
    }
}
