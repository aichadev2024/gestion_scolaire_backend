package com.gestionscolaire.gestion_scolaire_backend.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class IncidentDisciplineRequest {
    @NotNull(message = "L'élève est obligatoire")
    private Long eleveId;

    @NotNull(message = "La classe est obligatoire")
    private Long classeId;

    private Long classeMatiereId;

    @NotNull(message = "La date est obligatoire")
    private LocalDate date;

    @NotNull(message = "L'heure est obligatoire")
    private LocalTime heure;

    @NotBlank(message = "Le statut est obligatoire")
    private String statut;

    private String commentaire;
}
