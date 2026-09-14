package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RecapitulatifLigne {
    private String matricule;
    private String nom;
    private String prenom;
    private String classeNom;
    /** Moyenne des bulletins générés sur l'année scolaire demandée — null si aucun bulletin. */
    private Double moyenneAnnuelle;
    /** Pourcentage de présence sur l'ensemble des séances enregistrées pour cet élève. */
    private Double tauxPresence;
    private int nbAbsences;
    private int nbRetards;
    private String statut;
    private String statutInscription;
    private String statutPedagogique;
}
