package com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurResponse {
    private Long id;
    private String email;
    private String username;
    private String role;
    private Boolean estActif;
    private java.time.LocalDateTime dateCreation;
    private ProfilDto profil;
    private String etablissementNom;
}


