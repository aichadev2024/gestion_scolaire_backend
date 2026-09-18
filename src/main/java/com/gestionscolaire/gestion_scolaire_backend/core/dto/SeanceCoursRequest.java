package com.gestionscolaire.gestion_scolaire_backend.core.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SeanceCoursRequest {
    @NotNull(message = "La classe et la matière sont obligatoires")
    private Long classeMatiereId;

    @NotNull(message = "La date est obligatoire")
    private LocalDate date;

    private LocalTime heureDebut;

    private LocalTime heureFin;

    /** Absent = séance effectuée. */
    private Boolean effectue;

    private String titre;

    private String contenu;

    private String devoirs;

    private String motifNonEffectue;
}
