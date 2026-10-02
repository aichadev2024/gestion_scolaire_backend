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
import java.util.Comparator;
import java.util.List;

@Service
public class TarifPlanServiceImpl implements TarifPlanService {

    private final TarifPlanRepository tarifPlanRepository;

    public TarifPlanServiceImpl(TarifPlanRepository tarifPlanRepository) {
        this.tarifPlanRepository = tarifPlanRepository;
    }

    /**
     * Filet de sécurité au démarrage : si un palier manque (ex. base vidée manuellement pour
     * des tests, sans reset de l'historique Flyway — la migration d'origine ne se rejoue
     * jamais), on le recrée avec des valeurs par défaut sans jamais écraser un plan présent.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void assurerPlansParDefaut() {
        creerSiAbsent("PLAN_200", "Jusqu'à 200 élèves", "15000", 200);
        creerSiAbsent("PLAN_300", "Jusqu'à 300 élèves", "25000", 300);
        creerSiAbsent("ILLIMITE", "Élèves illimités", "35000", null);
    }

    private void creerSiAbsent(String code, String libelle, String prix, Integer maxEleves) {
        if (tarifPlanRepository.findByCodeIgnoreCase(code).isEmpty()) {
            tarifPlanRepository.save(TarifPlan.builder()
                    .code(code).libelle(libelle).prixMensuel(new BigDecimal(prix)).maxEleves(maxEleves).build());
        }
    }

    @Override
    public List<TarifPlanResponse> listerTous() {
        // Du plus petit au plus grand palier ; l'illimité (null) en dernier.
        return tarifPlanRepository.findAll().stream()
                .sorted(Comparator.comparing(TarifPlan::getMaxEleves, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TarifPlanResponse modifierPlan(String code, BigDecimal prixMensuel, Integer maxEleves, Integer maxEnseignants) {
        TarifPlan tarif = tarifPlanRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Plan tarifaire introuvable : " + code));
        tarif.setPrixMensuel(prixMensuel);
        tarif.setMaxEleves(maxEleves);
        tarif.setMaxEnseignants(maxEnseignants);
        tarif.setLibelle(maxEleves != null ? "Jusqu'à " + maxEleves + " élèves" : "Élèves illimités");
        return toResponse(tarifPlanRepository.save(tarif));
    }

    @Override
    public BigDecimal obtenirPrix(String code) {
        return tarifPlanRepository.findByCodeIgnoreCase(code)
                .map(TarifPlan::getPrixMensuel)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public Integer obtenirLimiteEleves(String code) {
        return tarifPlanRepository.findByCodeIgnoreCase(code)
                .map(TarifPlan::getMaxEleves)
                .orElse(null);
    }

    @Override
    public Integer obtenirLimiteEnseignants(String code) {
        return tarifPlanRepository.findByCodeIgnoreCase(code)
                .map(TarifPlan::getMaxEnseignants)
                .orElse(null);
    }

    @Override
    public String obtenirLibelle(String code) {
        return tarifPlanRepository.findByCodeIgnoreCase(code)
                .map(TarifPlan::getLibelle)
                .filter(l -> !l.isBlank())
                .orElse(code);
    }

    private TarifPlanResponse toResponse(TarifPlan t) {
        return TarifPlanResponse.builder()
                .code(t.getCode())
                .libelle(t.getLibelle() != null ? t.getLibelle() : t.getCode())
                .prixMensuel(t.getPrixMensuel())
                .maxEleves(t.getMaxEleves())
                .maxEnseignants(t.getMaxEnseignants())
                .build();
    }
}
