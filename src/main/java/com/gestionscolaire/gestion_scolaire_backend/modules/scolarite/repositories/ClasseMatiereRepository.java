package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.ClasseMatiere;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClasseMatiereRepository extends JpaRepository<ClasseMatiere, Long> {
    List<ClasseMatiere> findByClasseId(Long classeId);
    List<ClasseMatiere> findByEnseignantId(Long enseignantId);
    Optional<ClasseMatiere> findByClasseIdAndMatiereId(Long classeId, Long matiereId);
}


