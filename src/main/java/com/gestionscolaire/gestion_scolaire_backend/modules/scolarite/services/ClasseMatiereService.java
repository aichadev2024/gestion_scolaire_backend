package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.ClasseMatiere;

import java.util.List;
import java.util.Optional;

public interface ClasseMatiereService {
    ClasseMatiere assigner(Long classeId, Long matiereId, Long enseignantId, Double coefficient);
    ClasseMatiere modifier(Long id, Long enseignantId, Double coefficient);
    Optional<ClasseMatiere> trouverParId(Long id);
    List<ClasseMatiere> listerParClasse(Long classeId);
    void supprimer(Long id);
}


