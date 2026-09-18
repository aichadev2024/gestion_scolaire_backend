package com.gestionscolaire.gestion_scolaire_backend.core.dto;

import java.time.LocalDate;
import java.util.List;

/** Une ligne de la fiche de suivi d'émargement : un enseignant, un jour, avec les niveaux où il enseigne. */
public record EmargementEnseignantResponse(
        Long id,
        Long enseignantId,
        String matricule,
        String nom,
        String prenom,
        LocalDate date,
        String statut,
        String heureArrivee,
        String heureDepart,
        String remarques,
        List<String> niveaux
) {}
