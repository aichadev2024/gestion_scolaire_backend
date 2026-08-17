package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe;
import java.util.List;
import java.util.Optional;

public interface ClasseService {
    Classe creerClasse(Classe classe, Integer niveauId, Long enseignantPrincipalId);
    Classe modifierClasse(Long id, Classe classeDetails, Integer niveauId, Long enseignantPrincipalId);
    Optional<Classe> trouverParId(Long id);
    List<Classe> listerParNiveau(Integer niveauId);
    List<Classe> listerToutes();
}


