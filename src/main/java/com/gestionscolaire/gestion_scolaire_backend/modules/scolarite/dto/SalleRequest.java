package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SalleRequest {
    @NotBlank(message = "Le nom de la salle est obligatoire")
    private String nom;

    @Min(value = 1, message = "La capacité doit être positive")
    private Integer capacite;
}
