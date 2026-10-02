package com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "L'identifiant est obligatoire")
    private String identifiant;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String motDePasse;

    /** « mobile » quand la connexion vient de l'application mobile (Android ou web mobile) — facultatif. */
    private String client;
}


