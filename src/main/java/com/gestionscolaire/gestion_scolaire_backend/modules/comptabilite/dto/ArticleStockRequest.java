package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ArticleStockRequest {
    @NotBlank(message = "Le nom de l'article est obligatoire")
    private String nom;

    private String categorie;

    /** Un mot (pièce, boîte, ramette…), jamais un nombre seul : « 100 » ici se lisait « 1 100 » dans la colonne En stock. */
    @Pattern(regexp = "^\\s*$|.*\\p{L}.*", message = "L'unité doit être un mot (pièce, boîte, ramette…), pas un nombre. La quantité s'enregistre avec le bouton Entrée.")
    private String unite;

    @Min(value = 0, message = "Le seuil d'alerte ne peut pas être négatif")
    private Integer seuilAlerte;
}
