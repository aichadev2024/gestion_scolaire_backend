package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class StatistiquesEtablissementResponse {
    private int totalEleves;
    private int totalEnseignants;
    private int totalClasses;
    private int totalPersonnel;
    /** Somme des frais de scolarité définis × effectif de chaque classe concernée. */
    private double totalFraisAttendus;
    /** Somme de tous les paiements effectivement encaissés. */
    private double totalEncaisse;
    /** totalFraisAttendus - totalEncaisse. */
    private double soldeRestant;
    private String devise;
}
