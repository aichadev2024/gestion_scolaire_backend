package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NoteRequest {
    @NotNull(message = "L'élève est obligatoire")
    private Long eleveId;

    @NotNull(message = "La classe-matière est obligatoire")
    private Long classeMatiereId;

    @NotBlank(message = "La période est obligatoire")
    private String periode;

    @NotBlank(message = "Le type d'évaluation est obligatoire")
    private String typeEvaluation;

    @NotNull(message = "La valeur est obligatoire")
    private Double valeur;

    private Double noteMax = 20.0;
    private String appreciation;
}


