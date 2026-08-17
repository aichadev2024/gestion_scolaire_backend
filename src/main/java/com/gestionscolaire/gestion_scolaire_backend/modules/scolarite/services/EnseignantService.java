package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Enseignant;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import java.util.List;
import java.util.Optional;

public interface EnseignantService {
    Enseignant creerEnseignant(Enseignant enseignant, Profil profil);
    Enseignant modifierEnseignant(Long id, Enseignant enseignantDetails, Profil profilDetails);
    Optional<Enseignant> trouverParId(Long id);
    Optional<Enseignant> trouverParMatricule(String matricule);
    List<Enseignant> listerTous();
}


