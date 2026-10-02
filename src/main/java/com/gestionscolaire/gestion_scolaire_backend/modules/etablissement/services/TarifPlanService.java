package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.TarifPlanResponse;

import java.math.BigDecimal;
import java.util.List;

public interface TarifPlanService {
    List<TarifPlanResponse> listerTous();
    TarifPlanResponse modifierPlan(String code, BigDecimal prixMensuel, Integer maxEleves, Integer maxEnseignants, boolean mobileInclus);
    BigDecimal obtenirPrix(String code);
    /** Limite d'élèves actifs pour ce plan — null = illimité (ou plan introuvable). */
    Integer obtenirLimiteEleves(String code);
    /** Limite de comptes enseignants pour ce plan — null = illimité (ou plan introuvable). */
    Integer obtenirLimiteEnseignants(String code);
    /** L'application mobile est-elle incluse dans ce plan ? Vrai si le plan est inconnu (on ne coupe l'accès de personne par erreur). */
    boolean mobileInclus(String code);
    /** Nom lisible du plan (« Starter »), ou le code lui-même si inconnu. */
    String obtenirLibelle(String code);
}
