package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClasseResponse {
    private Long id;
    private String nom;
    private Integer niveauId;
    private String niveauNom;
    private Long enseignantPrincipalId;
    private String anneeScolaire;
    private Integer capaciteMax;
}


