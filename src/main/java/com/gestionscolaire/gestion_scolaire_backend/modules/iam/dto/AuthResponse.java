package com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type;
    private Long utilisateurId;
    private String email;
    private String username;
    private String role;
    private String prenom;
    private String nom;
    private Long etablissementId;
    private String etablissementNom;
    private Boolean requiresOtp;
    private String message;
}


