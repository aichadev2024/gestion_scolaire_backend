package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.TarifPlanResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.TarifPlan;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories.TarifPlanRepository;
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
