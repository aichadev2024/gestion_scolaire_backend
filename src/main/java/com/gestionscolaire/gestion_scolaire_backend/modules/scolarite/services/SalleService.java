package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Salle;

import java.util.List;
import java.util.Optional;

public interface SalleService {
    Salle creerSalle(Salle salle);
    Salle modifierSalle(Long id, Salle salleDetails);
    Optional<Salle> trouverParId(Long id);
    List<Salle> listerToutes();
    void supprimerSalle(Long id);
}
