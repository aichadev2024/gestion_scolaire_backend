package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.Paiement;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.repositories.PaiementRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.StatistiquesEtablissementResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.FraisScolarite;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EleveRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EnseignantRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.FraisScolariteRepository;
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

        int totalEleves = eleveRepository.findByEtablissementId(etabId).size();
        int totalEnseignants = enseignantRepository.findByEtablissementId(etabId).size();
        int totalClasses = classeRepository.findByEtablissementId(etabId).size();
        int totalPersonnel = utilisateurRepository.findByEtablissementId(etabId).size();

        List<FraisScolarite> fraisList = fraisScolariteRepository.findByEtablissementId(etabId);
        Map<Long, Integer> effectifParClasse = new HashMap<>();
        double totalFraisAttendus = 0;
        for (FraisScolarite frais : fraisList) {
            if (frais.getClasse() == null) continue;
            Long classeId = frais.getClasse().getId();
            int effectif = effectifParClasse.computeIfAbsent(classeId, id -> eleveRepository.findByClasseId(id).size());
            totalFraisAttendus += frais.getMontant() * effectif;
        }

        double totalEncaisse = paiementRepository.findByEtablissementId(etabId).stream()
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
