package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PromotionRapport {
    private int totalDemandes;
    private int succes;
    private int echecs;
    private List<PromotionLigneResultat> resultats;
}
