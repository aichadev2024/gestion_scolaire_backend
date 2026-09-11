package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.TarifPlanResponse;

import java.math.BigDecimal;
import java.util.List;

public interface TarifPlanService {
    List<TarifPlanResponse> listerTous();
    TarifPlanResponse modifierPrix(String code, BigDecimal nouveauPrix);
    BigDecimal obtenirPrix(String code);
}
