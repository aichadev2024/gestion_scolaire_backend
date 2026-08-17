package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import java.util.List;
import java.util.Optional;

public interface EleveService {
    Eleve inscrireEleve(Eleve eleve, Profil profil, Long parentId, Long classeId);
    Eleve modifierEleve(Long id, Eleve eleveDetails, Profil profilDetails);
    Optional<Eleve> trouverParId(Long id);
    Optional<Eleve> trouverParMatricule(String matricule);
    List<Eleve> listerElevesParClasse(Long classeId);
    List<Eleve> listerElevesParParent(Long parentId);
    List<Eleve> listerTous();
    void archiverEleve(Long id);
}


