package com.gestionscolaire.gestion_scolaire_backend.core.dto;

import java.util.List;

/**
 * Indicateurs de performance d'une classe sur une période (ou l'année) : bilan de la classe,
 * bilan par matière et classement des élèves avec la proposition de passage / redoublement.
 */
public record PerformanceClasseResponse(
        Long classeId,
        String classeNom,
        String anneeScolaire,
        String periode,
        double seuilPassage,
        double seuilRedoublement,
        int effectif,
        int elevesNotes,
        double moyenneClasse,
        double tauxReussite,
        double moyenneMin,
        double moyenneMax,
        List<MatierePerf> matieres,
        List<ElevePerf> eleves
) {
    public record MatierePerf(Long classeMatiereId, String matiereNom, String enseignantNom,
                              double moyenne, double tauxReussite, int elevesNotes) {}

    /** proposition : PASSAGE, A_DELIBERER, REDOUBLEMENT ou SANS_NOTES. decision : décision de la direction (ou null). */
    public record ElevePerf(Long eleveId, String matricule, String nom, String prenom,
                            Double moyenne, int rang, String proposition,
                            String decision, String commentaireDecision) {}
}
