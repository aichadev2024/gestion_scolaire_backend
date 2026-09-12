package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PromotionLigneResultat {
    private Long eleveId;
    private boolean succes;
    private String nomComplet;
    private String erreur;
}
