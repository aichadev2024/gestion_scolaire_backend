package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.TarifPlanResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.TarifPlan;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories.TarifPlanRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories.EtablissementRepository;
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
    private final EtablissementRepository etablissementRepository;

    public TarifPlanServiceImpl(TarifPlanRepository tarifPlanRepository, EtablissementRepository etablissementRepository) {
        this.tarifPlanRepository = tarifPlanRepository;
        this.etablissementRepository = etablissementRepository;
    }

    /**
     * Met les plans à niveau au démarrage, sans jamais écraser un réglage fait par le super-admin.
     *
     * En production Flyway est désactivé (le schéma suit les entités via ddl-auto=update) : les
     * migrations SQL de données n'y sont donc jamais exécutées, c'est ce code qui les remplace.
     * Il (re)crée les trois plans, complète les anciennes lignes STARTER/PRO (jusqu'ici limitées par
     * le nombre d'enseignants) et rebascule vers elles les écoles et plans provisoires
     * PLAN_200/PLAN_300/ILLIMITE d'un déploiement intermédiaire.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void assurerPlansParDefaut() {
        harmoniser("ESSENTIEL", "Essentiel", "15000", 200, false);
        harmoniser("STARTER", "Starter", "50000", 300, true);
        harmoniser("PRO", "Pro", "75000", null, true);

        // Jamais vers ESSENTIEL : une école existante y perdrait l'appli mobile qu'elle utilise.
        remplacerPlanProvisoire("PLAN_200", "STARTER");
        remplacerPlanProvisoire("PLAN_300", "STARTER");
        remplacerPlanProvisoire("ILLIMITE", "PRO");
    }

    /** Crée le plan s'il manque ; complète une ancienne ligne (sans nom) une seule fois, en gardant son prix. */
    private void harmoniser(String code, String libelle, String prix, Integer maxEleves, boolean mobileInclus) {
        TarifPlan plan = tarifPlanRepository.findByCodeIgnoreCase(code).orElse(null);
        if (plan == null) {
            tarifPlanRepository.save(TarifPlan.builder()
                    .code(code).libelle(libelle).prixMensuel(new BigDecimal(prix))
                    .maxEleves(maxEleves).mobileInclus(mobileInclus).build());
        } else if (plan.getLibelle() == null || plan.getLibelle().isBlank()) {
            plan.setLibelle(libelle);
            plan.setMaxEleves(maxEleves);
            plan.setMaxEnseignants(null);
            plan.setMobileInclus(mobileInclus);
            tarifPlanRepository.save(plan);
        }
    }

    private void remplacerPlanProvisoire(String ancien, String nouveau) {
        etablissementRepository.changerPlan(ancien, nouveau);
        tarifPlanRepository.findByCodeIgnoreCase(ancien).ifPresent(tarifPlanRepository::delete);
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
    public TarifPlanResponse modifierPlan(String code, BigDecimal prixMensuel, Integer maxEleves, Integer maxEnseignants, boolean mobileInclus) {
        TarifPlan tarif = tarifPlanRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Plan tarifaire introuvable : " + code));
        tarif.setPrixMensuel(prixMensuel);
        tarif.setMaxEleves(maxEleves);
        tarif.setMaxEnseignants(maxEnseignants);
        tarif.setMobileInclus(mobileInclus);
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
    public boolean mobileInclus(String code) {
        return tarifPlanRepository.findByCodeIgnoreCase(code)
                .map(t -> !Boolean.FALSE.equals(t.getMobileInclus()))
                .orElse(true);
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
                .mobileInclus(!Boolean.FALSE.equals(t.getMobileInclus()))
                .build();
    }
}
