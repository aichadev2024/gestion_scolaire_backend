package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.TarifPlanResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.TarifPlan;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories.TarifPlanRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TarifPlanServiceImpl implements TarifPlanService {

    private final TarifPlanRepository tarifPlanRepository;

    public TarifPlanServiceImpl(TarifPlanRepository tarifPlanRepository) {
        this.tarifPlanRepository = tarifPlanRepository;
    }

    /**
     * Filet de sécurité au démarrage : si STARTER/PRO manquent (ex. base vidée
     * manuellement pour des tests, sans reset de l'historique Flyway — la
     * migration d'origine ne se rejoue jamais), on les recrée avec des valeurs
     * par défaut sans jamais écraser un plan déjà présent.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void assurerPlansParDefaut() {
        if (tarifPlanRepository.findByCodeIgnoreCase("STARTER").isEmpty()) {
            tarifPlanRepository.save(TarifPlan.builder()
                    .code("STARTER").prixMensuel(new BigDecimal("50000")).maxEnseignants(12).build());
        }
        if (tarifPlanRepository.findByCodeIgnoreCase("PRO").isEmpty()) {
            tarifPlanRepository.save(TarifPlan.builder()
                    .code("PRO").prixMensuel(new BigDecimal("75000")).maxEnseignants(null).build());
        }
    }

    @Override
    public List<TarifPlanResponse> listerTous() {
        return tarifPlanRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TarifPlanResponse modifierPlan(String code, BigDecimal prixMensuel, Integer maxEnseignants) {
        TarifPlan tarif = tarifPlanRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Plan tarifaire introuvable : " + code));
        tarif.setPrixMensuel(prixMensuel);
        tarif.setMaxEnseignants(maxEnseignants);
        return toResponse(tarifPlanRepository.save(tarif));
    }

    @Override
    public BigDecimal obtenirPrix(String code) {
        return tarifPlanRepository.findByCodeIgnoreCase(code)
                .map(TarifPlan::getPrixMensuel)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public Integer obtenirLimiteEnseignants(String code) {
        return tarifPlanRepository.findByCodeIgnoreCase(code)
                .map(TarifPlan::getMaxEnseignants)
                .orElse(null);
    }

    private TarifPlanResponse toResponse(TarifPlan t) {
        return TarifPlanResponse.builder()
                .code(t.getCode())
                .prixMensuel(t.getPrixMensuel())
                .maxEnseignants(t.getMaxEnseignants())
                .build();
    }
}
