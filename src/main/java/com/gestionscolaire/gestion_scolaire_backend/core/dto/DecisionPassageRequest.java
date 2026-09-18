package com.gestionscolaire.gestion_scolaire_backend.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DecisionPassageRequest {
    @NotNull(message = "L'élève est obligatoire")
    private Long eleveId;

    /** PASSAGE, REDOUBLEMENT — ou AUCUNE pour retirer la décision. */
    @NotBlank(message = "La décision est obligatoire")
    private String decision;

    private String commentaire;
}
