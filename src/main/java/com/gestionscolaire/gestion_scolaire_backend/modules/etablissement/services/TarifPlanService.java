package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.TarifPlanResponse;

import java.math.BigDecimal;
import java.util.List;

public interface TarifPlanService {
    List<TarifPlanResponse> listerTous();
    TarifPlanResponse modifierPlan(String code, BigDecimal prixMensuel, Integer maxEnseignants);
    BigDecimal obtenirPrix(String code);
    /** Limite de comptes enseignants pour ce plan — null = illimité (ou plan introuvable). */
    Integer obtenirLimiteEnseignants(String code);
}
