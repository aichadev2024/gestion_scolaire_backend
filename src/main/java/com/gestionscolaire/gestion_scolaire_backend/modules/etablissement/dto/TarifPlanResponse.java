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
    private String libelle;
    private BigDecimal prixMensuel;
    /** Nombre max d'élèves actifs pour ce plan — null = illimité. */
    private Integer maxEleves;
    /** Nombre max de comptes enseignants pour ce plan — null = illimité. */
    private Integer maxEnseignants;
    private boolean mobileInclus;
}
