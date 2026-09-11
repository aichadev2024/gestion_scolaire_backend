package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class TarifPlanResponse {
    private String code;
    private BigDecimal prixMensuel;
}
