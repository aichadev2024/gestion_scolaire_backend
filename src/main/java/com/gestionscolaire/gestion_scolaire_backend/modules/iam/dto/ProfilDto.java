package com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilDto {
    private String prenom;
    private String nom;
    private String telephone;
    private String email;
    private String photoUrl;
    private String genre;
    private LocalDate dateNaissance;
    private String adresse;
}


