package com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterUtilisateurRequest {
    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    private String username;

    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String motDePasse;

    private String role;

    private ProfilDto profil;
}


