package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.Note;
import java.util.List;

public interface NoteService {
    Note enregistrerNote(Note note, Long eleveId, Long classeMatiereId, Long userCreateurId);
    List<Note> listerNotesEleve(Long eleveId);
    List<Note> listerNotesParClasseMatiere(Long classeMatiereId);
    Double calculerMoyenneEleveParMatiere(Long eleveId, Long classeMatiereId, String periode);
    Double calculerMoyenneGeneraleEleve(Long eleveId, String periode);
}


