package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.EffectiviteCoursResponse;
import com.gestionscolaire.gestion_scolaire_backend.core.dto.MonCoursResponse;
import com.gestionscolaire.gestion_scolaire_backend.core.dto.SeanceCoursRequest;
import com.gestionscolaire.gestion_scolaire_backend.core.dto.SeanceCoursResponse;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.ClasseMatiere;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.EmploiDuTemps;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Notification;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.SeanceCours;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseMatiereRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EmploiDuTempsRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EnseignantRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.SeanceCoursRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Cahier de texte (ce qui a été enseigné) et effectivité des cours face à l'emploi du temps. */
@Service
@Transactional
public class SeanceCoursService {

    private final SeanceCoursRepository seanceRepository;
    private final ClasseMatiereRepository classeMatiereRepository;
    private final EmploiDuTempsRepository emploiDuTempsRepository;
    private final EnseignantRepository enseignantRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EleveRepository eleveRepository;
    private final NotificationService notificationService;
    private final TenantGuard tenantGuard;

    public SeanceCoursService(SeanceCoursRepository seanceRepository,
                              ClasseMatiereRepository classeMatiereRepository,
                              EmploiDuTempsRepository emploiDuTempsRepository,
                              EnseignantRepository enseignantRepository,
                              UtilisateurRepository utilisateurRepository,
                              com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EleveRepository eleveRepository,
                              NotificationService notificationService,
                              TenantGuard tenantGuard) {
        this.seanceRepository = seanceRepository;
        this.classeMatiereRepository = classeMatiereRepository;
        this.emploiDuTempsRepository = emploiDuTempsRepository;
        this.enseignantRepository = enseignantRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.eleveRepository = eleveRepository;
        this.notificationService = notificationService;
        this.tenantGuard = tenantGuard;
    }

    private Integer niveauDe(ClasseMatiere cm) {
        return cm.getClasse() != null && cm.getClasse().getNiveau() != null ? cm.getClasse().getNiveau().getId() : null;
    }

    private Utilisateur utilisateurCourant() {
        try {
            return SecurityUtils.getCurrentUser().getUtilisateur();
        } catch (Exception e) {
            return null;
        }
    }

    /** Identifiant de la fiche enseignant si l'appelant est un ENSEIGNANT, sinon null (direction, secrétariat...). */
    private Long enseignantCourantId() {
        Utilisateur u = utilisateurCourant();
        if (u == null || u.getRole() == null || !"ENSEIGNANT".equalsIgnoreCase(u.getRole().getNom())) {
            return null;
        }
        return enseignantRepository.findByProfilUtilisateurId(u.getId())
                .map(e -> e.getId())
                .orElse(-1L);
    }

    private boolean estMonCours(ClasseMatiere cm, Long enseignantId) {
        return cm.getEnseignant() != null && enseignantId.equals(cm.getEnseignant().getId());
    }

    private ClasseMatiere classeMatierePourEcriture(Long classeMatiereId) {
        ClasseMatiere cm = tenantGuard.requireSameTenant(classeMatiereRepository.findById(classeMatiereId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe/matière introuvable")));
        tenantGuard.requireSameNiveau(cm, this::niveauDe);
        Long enseignantId = enseignantCourantId();
        if (enseignantId != null && !estMonCours(cm, enseignantId)) {
            throw new BadRequestException("Vous n'êtes pas l'enseignant assigné à cette matière pour cette classe.");
        }
        return cm;
    }

    private void appliquer(SeanceCours s, SeanceCoursRequest r) {
        boolean effectue = r.getEffectue() == null || r.getEffectue();
        if (r.getDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("La date d'une séance ne peut pas être dans le futur.");
        }
        if (effectue && (r.getContenu() == null || r.getContenu().isBlank()) && (r.getTitre() == null || r.getTitre().isBlank())) {
            throw new BadRequestException("Indiquez ce qui a été enseigné (titre ou contenu de la séance).");
        }
        s.setDate(r.getDate());
        s.setHeureDebut(r.getHeureDebut());
        s.setHeureFin(r.getHeureFin());
        s.setEffectue(effectue);
        s.setTitre(nettoyer(r.getTitre()));
        s.setContenu(nettoyer(r.getContenu()));
        s.setDevoirs(nettoyer(r.getDevoirs()));
        s.setMotifNonEffectue(effectue ? null : nettoyer(r.getMotifNonEffectue()));
    }

    private static String nettoyer(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    public SeanceCoursResponse creer(SeanceCoursRequest request) {
        ClasseMatiere cm = classeMatierePourEcriture(request.getClasseMatiereId());
        SeanceCours s = SeanceCours.builder().classeMatiere(cm).build();
        appliquer(s, request);
        Utilisateur u = utilisateurCourant();
        if (u != null) {
            utilisateurRepository.findById(u.getId()).ifPresent(s::setEnregistrePar);
        }
        SeanceCours saved = seanceRepository.save(s);
        notifierDevoirs(saved);
        notifierDirection(saved);
        return SeanceCoursResponse.de(saved);
    }

    /**
     * Prévient les directeurs (du niveau concerné) qu'un enseignant n'a pas assuré un cours, avec le motif.
     * Les séances effectuées ne notifient pas : elles restent consultables (cahier de texte, effectivité).
     * Rien n'est envoyé quand la séance est saisie par la direction elle-même.
     */
    private void notifierDirection(SeanceCours s) {
        try {
            if (Boolean.TRUE.equals(s.getEffectue()) || enseignantCourantId() == null) return;
            Long etablissementId = tenantGuard.etablissementId();
            if (etablissementId == null) return;
            ClasseMatiere cm = s.getClasseMatiere();
            String classe = cm.getClasse() != null ? cm.getClasse().getNom() : "Classe";
            String matiere = cm.getMatiere() != null ? cm.getMatiere().getNom() : "matière";
            String enseignant = "Un enseignant";
            if (cm.getEnseignant() != null && cm.getEnseignant().getProfil() != null) {
                enseignant = (cm.getEnseignant().getProfil().getPrenom() + " " + cm.getEnseignant().getProfil().getNom()).trim();
            }
            String date = s.getDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String titre = "Cours non effectué";
            String contenu = enseignant + " — " + classe + ", " + matiere + " (" + date + ") : cours non effectué"
                    + (s.getMotifNonEffectue() != null ? " (" + s.getMotifNonEffectue() + ")." : ".");
            Utilisateur auteur = utilisateurCourant();
            Long expediteurId = auteur != null ? auteur.getId() : null;
            for (Utilisateur u : utilisateurRepository.findByEtablissementId(etablissementId)) {
                if (u.getRole() == null || !"DIRECTEUR".equalsIgnoreCase(u.getRole().getNom())) continue;
                if (u.getNiveauSupervise() != null && cm.getClasse() != null && cm.getClasse().getNiveau() != null
                        && !u.getNiveauSupervise().getId().equals(cm.getClasse().getNiveau().getId())) continue;
                try {
                    notificationService.envoyerNotification(
                            Notification.builder().titre(titre).contenu(contenu).build(), expediteurId, u.getId());
                } catch (Exception ignored) {
                    // un destinataire en échec ne bloque pas les autres
                }
            }
        } catch (Exception ignored) {
            // la séance est enregistrée : la notification est un bonus
        }
    }

    public SeanceCoursResponse modifier(Long id, SeanceCoursRequest request) {
        SeanceCours s = tenantGuard.requireSameTenant(seanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Séance introuvable")));
        classeMatierePourEcriture(s.getClasseMatiere().getId());
        appliquer(s, request);
        return SeanceCoursResponse.de(seanceRepository.save(s));
    }

    public void supprimer(Long id) {
        SeanceCours s = tenantGuard.requireSameTenant(seanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Séance introuvable")));
        classeMatierePourEcriture(s.getClasseMatiere().getId());
        seanceRepository.delete(s);
    }

    @Transactional(readOnly = true)
    public List<SeanceCoursResponse> lister(Long classeId, LocalDate debut, LocalDate fin) {
        Long enseignantId = enseignantCourantId();
        return tenantGuard.filterSameNiveau(
                        tenantGuard.filterSameTenant(seanceRepository
                                .findByClasseMatiereClasseIdAndDateBetweenOrderByDateDescHeureDebutDesc(classeId, debut, fin)),
                        s -> niveauDe(s.getClasseMatiere()))
                .stream()
                .filter(s -> enseignantId == null || estMonCours(s.getClasseMatiere(), enseignantId))
                .map(SeanceCoursResponse::de)
                .toList();
    }

    /** Prévient les parents de la classe quand un enseignant donne des devoirs. */
    private void notifierDevoirs(SeanceCours s) {
        try {
            if (!Boolean.TRUE.equals(s.getEffectue()) || s.getDevoirs() == null || s.getDevoirs().isBlank()) return;
            ClasseMatiere cm = s.getClasseMatiere();
            if (cm.getClasse() == null) return;
            String matiere = cm.getMatiere() != null ? cm.getMatiere().getNom() : "une matière";
            String devoirs = s.getDevoirs().length() > 220 ? s.getDevoirs().substring(0, 220) + "…" : s.getDevoirs();
            String contenu = "Devoirs de " + matiere + " (séance du "
                    + s.getDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ") : " + devoirs;
            Utilisateur auteur = utilisateurCourant();
            Long expediteurId = auteur != null ? auteur.getId() : null;
            for (com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve e : eleveRepository.findByClasseId(cm.getClasse().getId())) {
                if (e.getStatut() != null && !"ACTIF".equalsIgnoreCase(e.getStatut())) continue;
                notificationService.notifierParentsEleve(e, "Nouveaux devoirs", contenu, expediteurId);
            }
        } catch (Exception ignored) {
            // la séance est enregistrée : la notification est un bonus
        }
    }

    /** Cahier de texte de la classe d'un élève, visible par ses parents et par lui-même (séances effectuées seulement). */
    @Transactional(readOnly = true)
    public List<SeanceCoursResponse> listerPourEleve(Long eleveId, LocalDate debut, LocalDate fin) {
        com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve eleve = tenantGuard.requireSameTenant(
                eleveRepository.findById(eleveId).orElseThrow(() -> new ResourceNotFoundException("Élève introuvable")));
        verifierAccesFamille(eleve);
        if (eleve.getClasse() == null) return List.of();
        return seanceRepository
                .findByClasseMatiereClasseIdAndDateBetweenOrderByDateDescHeureDebutDesc(eleve.getClasse().getId(), debut, fin)
                .stream()
                .filter(x -> Boolean.TRUE.equals(x.getEffectue()))
                .map(SeanceCoursResponse::de)
                .toList();
    }

    /** Un parent ne voit que ses enfants, un élève que lui-même ; le personnel voit tout l'établissement. */
    private void verifierAccesFamille(com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve eleve) {
        Utilisateur courant = utilisateurCourant();
        if (courant == null || courant.getRole() == null) return;
        String role = courant.getRole().getNom();
        boolean concerne;
        if ("PARENT".equalsIgnoreCase(role)) {
            concerne = estCompteDe(eleve.getParent(), courant) || estCompteDe(eleve.getParentSecondaire(), courant);
        } else if ("ELEVE".equalsIgnoreCase(role)) {
            concerne = eleve.getProfil() != null && eleve.getProfil().getUtilisateur() != null
                    && courant.getId().equals(eleve.getProfil().getUtilisateur().getId());
        } else {
            return;
        }
        if (!concerne) throw new ResourceNotFoundException("Élève introuvable");
    }

    private static boolean estCompteDe(com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Parent parent, Utilisateur courant) {
        return parent != null && parent.getProfil() != null && parent.getProfil().getUtilisateur() != null
                && courant.getId().equals(parent.getProfil().getUtilisateur().getId());
    }

    @Transactional(readOnly = true)
    public List<MonCoursResponse> mesCours() {
        Long enseignantId = enseignantCourantId();
        if (enseignantId == null || enseignantId < 0) {
            return List.of();
        }
        return classeMatiereRepository.findByEnseignantId(enseignantId).stream()
                .map(cm -> new MonCoursResponse(cm.getId(),
                        cm.getClasse() != null ? cm.getClasse().getId() : null,
                        cm.getClasse() != null ? cm.getClasse().getNom() : null,
                        cm.getMatiere() != null ? cm.getMatiere().getNom() : null,
                        cm.getClasse() != null && cm.getClasse().getNiveau() != null ? cm.getClasse().getNiveau().getNom() : null))
                .sorted(Comparator.comparing(MonCoursResponse::classeNom, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EffectiviteCoursResponse> effectivite(LocalDate debut, LocalDate fin) {
        if (fin.isBefore(debut)) {
            throw new BadRequestException("La date de fin doit suivre la date de début.");
        }
        Long etablissementId = tenantGuard.requireEtablissementId();
        LocalDate limite = fin.isAfter(LocalDate.now()) ? LocalDate.now() : fin;

        // Nombre de jours de chaque jour de semaine (1 = lundi … 7 = dimanche) dans [debut, limite].
        int[] joursParSemaine = new int[8];
        for (LocalDate d = debut; !d.isAfter(limite); d = d.plusDays(1)) {
            joursParSemaine[d.getDayOfWeek().getValue()]++;
        }

        Map<Long, Integer> prevues = new HashMap<>();
        Map<Long, ClasseMatiere> cours = new HashMap<>();
        for (EmploiDuTemps cr : emploiDuTempsRepository.findByEtablissementId(etablissementId)) {
            if (cr.getClasseMatiere() == null || !"COURS".equalsIgnoreCase(cr.getTypeCreneau())) continue;
            if (cr.getJourSemaine() == null || cr.getJourSemaine() < 1 || cr.getJourSemaine() > 7) continue;
            ClasseMatiere cm = cr.getClasseMatiere();
            prevues.merge(cm.getId(), joursParSemaine[cr.getJourSemaine()], Integer::sum);
            cours.put(cm.getId(), cm);
        }

        Map<Long, int[]> declarees = new HashMap<>(); // [effectuees, nonEffectuees]
        for (SeanceCours s : seanceRepository.findByEtablissementIdAndDateBetween(etablissementId, debut, limite)) {
            ClasseMatiere cm = s.getClasseMatiere();
            cours.put(cm.getId(), cm);
            int[] c = declarees.computeIfAbsent(cm.getId(), k -> new int[2]);
            if (Boolean.TRUE.equals(s.getEffectue())) c[0]++; else c[1]++;
        }

        List<EffectiviteCoursResponse> resultat = new ArrayList<>();
        for (ClasseMatiere cm : tenantGuard.filterSameNiveau(cours.values(), this::niveauDe)) {
            int p = prevues.getOrDefault(cm.getId(), 0);
            int[] c = declarees.getOrDefault(cm.getId(), new int[2]);
            int effectuees = c[0];
            int nonEffectuees = c[1];
            int base = Math.max(p, effectuees + nonEffectuees);
            int nonRenseignees = Math.max(0, base - effectuees - nonEffectuees);
            double taux = base == 0 ? 0.0 : Math.round(1000.0 * effectuees / base) / 10.0;
            resultat.add(new EffectiviteCoursResponse(
                    cm.getId(),
                    cm.getClasse() != null ? cm.getClasse().getNom() : null,
                    cm.getMatiere() != null ? cm.getMatiere().getNom() : null,
                    SeanceCoursResponse.nomEnseignant(cm),
                    base, effectuees, nonEffectuees, nonRenseignees, taux));
        }
        resultat.sort(Comparator.comparingDouble(EffectiviteCoursResponse::tauxEffectivite));
        return resultat;
    }
}
