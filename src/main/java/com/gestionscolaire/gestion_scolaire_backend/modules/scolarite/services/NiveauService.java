package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Niveau;

import java.util.List;
import java.util.Optional;

public interface NiveauService {
    Niveau creer(String nom);
    Optional<Niveau> trouverParId(Integer id);
    List<Niveau> listerTous();
}


