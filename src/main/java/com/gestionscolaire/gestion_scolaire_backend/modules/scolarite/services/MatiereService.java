package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Matiere;

import java.util.List;
import java.util.Optional;

public interface MatiereService {
    Matiere creerMatiere(Matiere matiere);
    Matiere modifierMatiere(Long id, Matiere matiereDetails);
    Optional<Matiere> trouverParId(Long id);
    Optional<Matiere> trouverParCode(String code);
    List<Matiere> listerToutes();
    void supprimerMatiere(Long id);
}


