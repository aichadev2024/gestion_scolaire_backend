package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.Note;
import java.util.List;
import java.util.Map;

public interface NoteService {
    Note enregistrerNote(Note note, Long eleveId, Long classeMatiereId, Long userCreateurId);
    List<Note> listerNotesEleve(Long eleveId);
    List<Note> listerNotesParClasseMatiere(Long classeMatiereId);
    Double calculerMoyenneEleveParMatiere(Long eleveId, Long classeMatiereId, String periode);
    Double calculerMoyenneGeneraleEleve(Long eleveId, String periode);

    /** Moyenne de chaque période (composition/trimestre...) où l'élève a au moins une note, pour cette matière. */
    Map<String, Double> moyennesParPeriodeMatiere(Long eleveId, Long classeMatiereId);
    /** Moyenne annuelle d'une matière = moyenne des moyennes de chaque période de l'année (pas une moyenne brute de toutes les notes). */
    Double calculerMoyenneAnnuelleMatiere(Long eleveId, Long classeMatiereId);
    /** Moyenne générale annuelle de l'élève, pondérée par les coefficients — pour le bulletin de fin d'année. */
    Double calculerMoyenneGeneraleAnnuelleEleve(Long eleveId);
}


