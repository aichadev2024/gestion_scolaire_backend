package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.Paiement;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.repositories.PaiementRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.StatistiquesEtablissementResponse;
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
        List<Eleve> eleves = tenantGuard.filterSameNiveau(eleveRepository.findByEtablissementId(etabId), this::niveauDe);
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
            int effectif = effectifParClasse.computeIfAbsent(classeId, id -> eleveRepository.findByClasseId(id).size());
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
}
