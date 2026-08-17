package com.gestionscolaire.gestion_scolaire_backend.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PresenceRequest {
    @NotNull(message = "L'élève est obligatoire")
    private Long eleveId;

    private Long classeMatiereId;

    @NotNull(message = "La date est obligatoire")
    private LocalDate date;

    @NotBlank(message = "Le statut est obligatoire")
    private String statut;

    private Boolean estJustifie = false;
    private String notesJustification;
}


