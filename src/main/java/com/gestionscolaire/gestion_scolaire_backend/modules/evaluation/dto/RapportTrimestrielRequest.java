package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RapportTrimestrielRequest {
    /** Absent = rapport de l'ensemble de l'établissement. */
    private Long classeId;
    @NotBlank private String periode;
    @NotBlank private String anneeScolaire;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    @Size(max = 6000, message = "Le commentaire est limité à 6000 caractères.")
    private String commentaire;
}
