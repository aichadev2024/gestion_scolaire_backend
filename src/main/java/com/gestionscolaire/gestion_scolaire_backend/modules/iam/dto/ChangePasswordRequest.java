package com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank
    private String ancienMotDePasse;

    @NotBlank
    @Size(min = 6, message = "Le nouveau mot de passe doit contenir au moins 6 caractères")
    private String nouveauMotDePasse;
}


