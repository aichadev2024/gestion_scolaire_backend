package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EleveImportLigneResultat {
    private int ligne;
    private boolean succes;
    private String matricule;
    private String nomComplet;
    private String motDePasseInitial;
    private String erreur;
}
