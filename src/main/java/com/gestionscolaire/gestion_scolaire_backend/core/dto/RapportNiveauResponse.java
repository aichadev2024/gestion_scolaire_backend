package com.gestionscolaire.gestion_scolaire_backend.core.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RapportNiveauResponse(
        Long id,
        Long classeMatiereId,
        Long classeId,
        String classeNom,
        String matiereNom,
        String enseignantNom,
        String auteur,
        String periode,
        String niveauGlobal,
        String commentaire,
        boolean estTraite,
        String reponseDirection,
        LocalDateTime dateCreation,
        LocalDateTime dateTraitement,
        List<EleveSignale> eleves
) {
    public record EleveSignale(Long eleveId, String matricule, String nom, String prenom, Double moyenne, String commentaire) {}

    /** Élève de la classe dont la moyenne dans la matière est sous le seuil (aide à la saisie du rapport). */
    public record EleveEnDifficulte(Long eleveId, String matricule, String nom, String prenom, double moyenne) {}
}
