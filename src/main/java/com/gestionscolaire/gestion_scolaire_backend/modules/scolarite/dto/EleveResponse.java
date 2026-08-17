package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto.ProfilDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EleveResponse {
    private Long id;
    private String matricule;
    private String statut;
    private Long classeId;
    private String classeNom;
    private Long parentId;
    private ProfilDto profil;
    private String etablissementNom;
}


