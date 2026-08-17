package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.EmploiDuTemps;

import java.util.List;
import java.util.Optional;

public interface EmploiDuTempsService {
    EmploiDuTemps creerCreneau(EmploiDuTemps creneau, Long classeMatiereId);
    EmploiDuTemps modifierCreneau(Long id, EmploiDuTemps creneauDetails, Long classeMatiereId);
    Optional<EmploiDuTemps> trouverParId(Long id);
    List<EmploiDuTemps> listerParClasse(Long classeId);
    List<EmploiDuTemps> listerParEnseignant(Long enseignantId);
    void supprimerCreneau(Long id);
}


