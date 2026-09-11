package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Renouvellement d'abonnement : plan choisi + durée en mois payée. */
@Data
public class RenouvellementRequest {

    @NotBlank(message = "Le plan tarifaire est obligatoire")
    private String planTarifaire; // STARTER, PRO

    @Min(value = 1, message = "La durée doit être d'au moins 1 mois")
    private int dureeMois;
}
