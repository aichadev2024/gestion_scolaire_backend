package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByEleveId(Long eleveId);
    List<Note> findByClasseMatiereId(Long classeMatiereId);
    List<Note> findByEleveIdAndClasseMatiereId(Long eleveId, Long classeMatiereId);
}


