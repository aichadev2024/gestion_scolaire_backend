package com.gestionscolaire.gestion_scolaire_backend.core.dto;

/**
 * Effectivité des cours d'une matière dans une classe sur une période : séances prévues à l'emploi
 * du temps (occurrences jusqu'à aujourd'hui) face aux séances renseignées dans le cahier de texte.
 */
public record EffectiviteCoursResponse(
        Long classeMatiereId,
        String classeNom,
        String matiereNom,
        String enseignantNom,
        int prevues,
        int effectuees,
        int nonEffectuees,
        int nonRenseignees,
        double tauxEffectivite
) {}
