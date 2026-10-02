package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.TarifPlanResponse;

import java.math.BigDecimal;
import java.util.List;

public interface TarifPlanService {
    String PLAN_PAR_DEFAUT = "PLAN_200";

    List<TarifPlanResponse> listerTous();
    TarifPlanResponse modifierPlan(String code, BigDecimal prixMensuel, Integer maxEleves, Integer maxEnseignants);
    BigDecimal obtenirPrix(String code);
    /** Limite d'élèves actifs pour ce plan — null = illimité (ou plan introuvable). */
    Integer obtenirLimiteEleves(String code);
    /** Limite de comptes enseignants pour ce plan — null = illimité (ou plan introuvable). */
    Integer obtenirLimiteEnseignants(String code);
    /** Libellé lisible du plan (« Jusqu'à 200 élèves »), ou le code lui-même si inconnu. */
    String obtenirLibelle(String code);
}
