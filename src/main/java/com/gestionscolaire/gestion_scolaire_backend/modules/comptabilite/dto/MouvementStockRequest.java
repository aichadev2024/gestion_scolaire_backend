package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MouvementStockRequest {
    @NotNull(message = "L'article est obligatoire")
    private Long articleId;

    @NotBlank(message = "Le type de mouvement est obligatoire")
    private String type; // ENTREE ou SORTIE

    @NotNull(message = "La quantité est obligatoire")
    @Min(value = 1, message = "La quantité doit être au moins 1")
    private Integer quantite;

    private LocalDate date;

    private String motif;

    private String beneficiaire;
}
