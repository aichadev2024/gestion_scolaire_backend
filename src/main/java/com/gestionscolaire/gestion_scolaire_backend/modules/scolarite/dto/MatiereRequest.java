package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MatiereRequest {
    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    private String code;
}


