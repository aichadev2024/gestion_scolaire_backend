package com.gestionscolaire.gestion_scolaire_backend.core.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PresenceEnseignantRequest {
    @NotNull(message = "L'ID de l'enseignant est obligatoire")
    private Long enseignantId;

    @NotNull(message = "La date est obligatoire")
    private LocalDate date;

    @NotNull(message = "Le statut est obligatoire")
    private String statut; // PRESENT, ABSENT, RETARD, CONGE

    private String heureArrivee;
    private String heureDepart;
    private String remarques;
}
