package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class EleveImportRapport {
    private int totalLignes;
    private int succes;
    private int echecs;
    private List<EleveImportLigneResultat> resultats;
}
