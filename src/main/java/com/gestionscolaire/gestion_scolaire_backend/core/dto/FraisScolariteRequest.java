package com.gestionscolaire.gestion_scolaire_backend.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FraisScolariteRequest {
    @NotNull(message = "La classe est obligatoire")
    private Long classeId;

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    @NotNull(message = "Le montant est obligatoire")
    private Double montant;

    @NotNull(message = "La date d'échéance est obligatoire")
    private LocalDate dateEcheance;
}


