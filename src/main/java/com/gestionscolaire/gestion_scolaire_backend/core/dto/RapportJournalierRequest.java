package com.gestionscolaire.gestion_scolaire_backend.core.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RapportJournalierRequest {
    @NotNull(message = "L'élève est obligatoire")
    private Long eleveId;

    @NotNull(message = "La date est obligatoire")
    private LocalDate date;

    private String repas;
    private Boolean siesteFaite;
    private Integer dureeSiesteMinutes;
    private Integer changesCouches;
    private String humeur;
    private String notes;
}
