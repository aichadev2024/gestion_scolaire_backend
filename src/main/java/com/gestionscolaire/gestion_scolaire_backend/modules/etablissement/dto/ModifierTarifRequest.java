package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ModifierTarifRequest {
    @NotNull(message = "Le prix mensuel est obligatoire")
    @Positive(message = "Le prix mensuel doit être supérieur à zéro")
    private BigDecimal prixMensuel;

    /** Nombre max de comptes enseignants — laisser vide/null pour illimité. */
    @Positive(message = "La limite d'enseignants doit être supérieure à zéro")
    private Integer maxEnseignants;
}
