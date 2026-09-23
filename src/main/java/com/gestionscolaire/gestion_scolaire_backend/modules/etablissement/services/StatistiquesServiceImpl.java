package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.Paiement;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.repositories.PaiementRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.StatistiquesEtablissementResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.StatistiquesFinancesResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Enseignant;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.FraisScolarite;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EleveRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EnseignantRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.FraisScolariteRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatistiquesServiceImpl implements StatistiquesService {

    private final TenantGuard tenantGuard;
    private final EleveRepository eleveRepository;
    private final EnseignantRepository enseignantRepository;
    private final ClasseRepository classeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final FraisScolariteRepository fraisScolariteRepository;
    private final PaiementRepository paiementRepository;

    public StatistiquesServiceImpl(
            TenantGuard tenantGuard,
            EleveRepository eleveRepository,
            EnseignantRepository enseignantRepository,
            ClasseRepository classeRepository,
            UtilisateurRepository utilisateurRepository,
            FraisScolariteRepository fraisScolariteRepository,
            PaiementRepository paiementRepository
    ) {
        this.tenantGuard = tenantGuard;
        this.eleveRepository = eleveRepository;
        this.enseignantRepository = enseignantRepository;
        this.classeRepository = classeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.fraisScolariteRepository = fraisScolariteRepository;
        this.paiementRepository = paiementRepository;
    }

    private Integer niveauDe(Eleve e) {
        return (e.getClasse() != null && e.getClasse().getNiveau() != null) ? e.getClasse().getNiveau().getId() : null;
    }

    @Override
    public StatistiquesEtablissementResponse obtenirPourEtablissementCourant() {
        Etablissement etablissement;
        try {
            etablissement = SecurityUtils.getCurrentUser().getUtilisateur().getEtablissement();
        } catch (Exception e) {
            etablissement = null;
        }
        if (etablissement == null) {
            throw new BadRequestException("Aucun établissement rattaché à ce compte.");
        }
        Long etabId = tenantGuard.requireEtablissementId();

        // Un directeur/censeur restreint à un niveau (le cas normal depuis la séparation par
        // niveau) ne voit QUE les chiffres de son niveau — jamais ceux de l'établissement entier.
        List<Classe> classes = tenantGuard.filterSameNiveau(classeRepository.findByEtablissementId(etabId),
                c -> c.getNiveau() != null ? c.getNiveau().getId() : null);
        // Un élève archivé (parti, radié...) ne doit plus compter dans l'effectif ni dans les frais
        // attendus — il ne sera plus facturé. Même filtre que listerRetardsPaiement().
        List<Eleve> eleves = tenantGuard.filterSameNiveau(eleveRepository.findByEtablissementId(etabId), this::niveauDe)
                .stream()
                .filter(e -> "ACTIF".equalsIgnoreCase(e.getStatut()))
                .toList();
        List<Enseignant> enseignants = tenantGuard.niveauRestreint()
                ? enseignantRepository.findByEtablissementId(etabId).stream()
                        .filter(en -> classeRepository.findByEnseignantPrincipalId(en.getId()).stream()
                                .anyMatch(c -> tenantGuard.correspondAuNiveauCourant(c.getNiveau() != null ? c.getNiveau().getId() : null)))
                        .toList()
                : enseignantRepository.findByEtablissementId(etabId);
        // Le personnel = les comptes de l'équipe ; les parents et les élèves ont aussi un compte mais n'en font pas partie.
        List<Utilisateur> personnel = utilisateurRepository.findByEtablissementId(etabId).stream()
                .filter(u -> u.getRole() != null && !List.of("PARENT", "ELEVE", "SUPER_ADMIN", "PROMOTEUR").contains(u.getRole().getNom()))
                .filter(u -> !tenantGuard.niveauRestreint()
                        || (u.getNiveauSupervise() != null && tenantGuard.correspondAuNiveauCourant(u.getNiveauSupervise().getId())))
                .toList();

        int totalEleves = eleves.size();
        int totalEnseignants = enseignants.size();
        int totalClasses = classes.size();
        int totalPersonnel = personnel.size();

        List<FraisScolarite> fraisList = tenantGuard.filterSameNiveau(
                fraisScolariteRepository.findByEtablissementId(etabId),
                f -> f.getClasse() != null && f.getClasse().getNiveau() != null ? f.getClasse().getNiveau().getId() : null);
        Map<Long, Integer> effectifParClasse = new HashMap<>();
        double totalFraisAttendus = 0;
        for (FraisScolarite frais : fraisList) {
            if (frais.getClasse() == null) continue;
            Long classeId = frais.getClasse().getId();
            int effectif = effectifParClasse.computeIfAbsent(classeId, id -> (int) eleveRepository.findByClasseId(id).stream()
                    .filter(e -> "ACTIF".equalsIgnoreCase(e.getStatut()))
                    .count());
            totalFraisAttendus += frais.getMontant() * effectif;
        }

        double totalEncaisse = tenantGuard.filterSameNiveau(paiementRepository.findByEtablissementId(etabId),
                        p -> p.getEleve() != null ? niveauDe(p.getEleve()) : null)
                .stream()
                .mapToDouble(Paiement::getMontantPaye)
                .sum();

        return StatistiquesEtablissementResponse.builder()
                .totalEleves(totalEleves)
                .totalEnseignants(totalEnseignants)
                .totalClasses(totalClasses)
                .totalPersonnel(totalPersonnel)
                .totalFraisAttendus(totalFraisAttendus)
                .totalEncaisse(totalEncaisse)
                .soldeRestant(totalFraisAttendus - totalEncaisse)
                .devise(etablissement.getDevise() != null ? etablissement.getDevise() : "FCFA")
                .build();
    }

    private static final String[] MOIS_FR = {"janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc."};

    /** Même classement que PaiementServiceImpl.typeDeFrais — INSCRIPTION, MENSUALITE ou AUTRE selon l'intitulé. */
    private static String typeDeFrais(String titre) {
        String t = titre == null ? "" : java.text.Normalizer.normalize(titre, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase();
        if (t.contains("inscri")) return "INSCRIPTION";
        if (t.contains("mensu") || t.contains("mois")
                || t.matches(".*(janvier|fevrier|mars|avril|mai|juin|juillet|aout|septembre|octobre|novembre|decembre).*")) {
            return "MENSUALITE";
        }
        return "AUTRE";
    }

    @Override
    public StatistiquesFinancesResponse obtenirFinancesDetaillees() {
        Etablissement etablissement;
        try {
            etablissement = SecurityUtils.getCurrentUser().getUtilisateur().getEtablissement();
        } catch (Exception e) {
            etablissement = null;
        }
        if (etablissement == null) {
            throw new BadRequestException("Aucun établissement rattaché à ce compte.");
        }
        Long etabId = tenantGuard.requireEtablissementId();
        String devise = etablissement.getDevise() != null ? etablissement.getDevise() : "FCFA";

        List<Eleve> eleves = tenantGuard.filterSameNiveau(eleveRepository.findByEtablissementId(etabId), this::niveauDe)
                .stream()
                .filter(e -> "ACTIF".equalsIgnoreCase(e.getStatut()))
                .toList();

        List<FraisScolarite> fraisList = tenantGuard.filterSameNiveau(
                fraisScolariteRepository.findByEtablissementId(etabId),
                f -> f.getClasse() != null && f.getClasse().getNiveau() != null ? f.getClasse().getNiveau().getId() : null);
        Map<Long, Integer> effectifParClasse = new HashMap<>();
        double totalFraisAttendus = 0;
        for (FraisScolarite frais : fraisList) {
            if (frais.getClasse() == null) continue;
            Long classeId = frais.getClasse().getId();
            int effectif = effectifParClasse.computeIfAbsent(classeId, id -> (int) eleveRepository.findByClasseId(id).stream()
                    .filter(e -> "ACTIF".equalsIgnoreCase(e.getStatut()))
                    .count());
            totalFraisAttendus += frais.getMontant() * effectif;
        }

        List<Paiement> paiements = tenantGuard.filterSameNiveau(paiementRepository.findByEtablissementId(etabId),
                        p -> p.getEleve() != null ? niveauDe(p.getEleve()) : null)
                .stream()
                .sorted(java.util.Comparator.comparing(Paiement::getDatePaiement).reversed())
                .toList();

        double totalEncaisse = paiements.stream().mapToDouble(Paiement::getMontantPaye).sum();

        java.time.LocalDateTime maintenant = java.time.LocalDateTime.now();
        double encaisseMoisCourant = paiements.stream()
                .filter(p -> p.getDatePaiement() != null
                        && p.getDatePaiement().getYear() == maintenant.getYear()
                        && p.getDatePaiement().getMonthValue() == maintenant.getMonthValue())
                .mapToDouble(Paiement::getMontantPaye).sum();
        double encaisseAnneeCourante = paiements.stream()
                .filter(p -> p.getDatePaiement() != null && p.getDatePaiement().getYear() == maintenant.getYear())
                .mapToDouble(Paiement::getMontantPaye).sum();

        // Courbe des 12 derniers mois (mois courant inclus), même sans aucun encaissement — utile
        // pour voir les mois « à zéro », pas seulement ceux où de l'argent est rentré.
        List<StatistiquesFinancesResponse.MoisMontant> parMois = new java.util.ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            java.time.YearMonth ym = java.time.YearMonth.from(maintenant).minusMonths(i);
            double montantMois = paiements.stream()
                    .filter(p -> p.getDatePaiement() != null && java.time.YearMonth.from(p.getDatePaiement()).equals(ym))
                    .mapToDouble(Paiement::getMontantPaye).sum();
            String libelle = MOIS_FR[ym.getMonthValue() - 1];
            parMois.add(new StatistiquesFinancesResponse.MoisMontant(ym.toString(), libelle, montantMois));
        }

        // Relevé détaillé — les 200 paiements les plus récents suffisent à une lecture complète
        // au quotidien ; au-delà, la page Finances de la comptabilité reste l'outil de recherche.
        List<StatistiquesFinancesResponse.PaiementDetail> detail = paiements.stream()
                .limit(200)
                .map(p -> {
                    Eleve e = p.getEleve();
                    String nom = e != null && e.getProfil() != null ? e.getProfil().getNom() : null;
                    String prenom = e != null && e.getProfil() != null ? e.getProfil().getPrenom() : null;
                    String matricule = e != null ? e.getMatricule() : null;
                    String classeNom = e != null && e.getClasse() != null ? e.getClasse().getNom() : null;
                    String fraisTitre = p.getFraisScolarite() != null ? p.getFraisScolarite().getTitre() : "Paiement";
                    String type = p.getFraisScolarite() != null ? typeDeFrais(p.getFraisScolarite().getTitre()) : "AUTRE";
                    return new StatistiquesFinancesResponse.PaiementDetail(
                            p.getId(), e != null ? e.getId() : null, nom, prenom, matricule, classeNom,
                            fraisTitre, type, p.getMontantPaye() != null ? p.getMontantPaye() : 0,
                            p.getDatePaiement(), p.getModePaiement(), p.getNumeroRecu());
                })
                .toList();

        return new StatistiquesFinancesResponse(
                devise, totalFraisAttendus, totalEncaisse, totalFraisAttendus - totalEncaisse,
                encaisseMoisCourant, encaisseAnneeCourante, parMois, detail);
    }
}
