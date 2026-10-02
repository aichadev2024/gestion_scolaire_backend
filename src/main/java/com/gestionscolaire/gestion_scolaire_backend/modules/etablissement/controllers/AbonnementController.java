package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.AbonnementResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories.EtablissementRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services.TarifPlanService;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EleveRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Abonnement de l'établissement de l'utilisateur connecté (compteur « 182 / 200 élèves »). */
@RestController
@RequestMapping("/api/abonnement")
public class AbonnementController {

    private final EtablissementRepository etablissementRepository;
    private final EleveRepository eleveRepository;
    private final TarifPlanService tarifPlanService;
    private final TenantGuard tenantGuard;

    public AbonnementController(EtablissementRepository etablissementRepository, EleveRepository eleveRepository,
                                TarifPlanService tarifPlanService, TenantGuard tenantGuard) {
        this.etablissementRepository = etablissementRepository;
        this.eleveRepository = eleveRepository;
        this.tarifPlanService = tarifPlanService;
        this.tenantGuard = tenantGuard;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<AbonnementResponse> monAbonnement() {
        Long etablissementId = tenantGuard.requireEtablissementId();
        Etablissement etab = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResourceNotFoundException("Établissement introuvable"));
        String plan = etab.getPlanTarifaire();
        return ResponseEntity.ok(AbonnementResponse.builder()
                .plan(plan)
                .libelle(tarifPlanService.obtenirLibelle(plan))
                .prixMensuel(tarifPlanService.obtenirPrix(plan))
                .maxEleves(tarifPlanService.obtenirLimiteEleves(plan))
                .elevesActifs(eleveRepository.compterActifs(etablissementId))
                .dateExpiration(etab.getDateExpirationAbonnement())
                .build());
    }
}
