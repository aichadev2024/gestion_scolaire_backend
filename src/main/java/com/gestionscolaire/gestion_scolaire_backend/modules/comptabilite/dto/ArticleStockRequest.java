package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ArticleStockRequest {
    @NotBlank(message = "Le nom de l'article est obligatoire")
    private String nom;

    private String categorie;

    private String unite;

    @Min(value = 0, message = "Le seuil d'alerte ne peut pas être négatif")
    private Integer seuilAlerte;
}
