package com.gestionscolaire.gestion_scolaire_backend.core.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Justification d'une absence/d'un retard, envoyée par le parent. */
@Data
public class PresenceJustificationRequest {

    @NotBlank(message = "Le motif de justification est obligatoire")
    private String notesJustification;
}
