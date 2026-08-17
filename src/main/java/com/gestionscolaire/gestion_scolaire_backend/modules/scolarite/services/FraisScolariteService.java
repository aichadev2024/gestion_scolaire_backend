package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.FraisScolarite;

import java.util.List;
import java.util.Optional;

public interface FraisScolariteService {
    FraisScolarite creerFrais(FraisScolarite frais, Long classeId);
    FraisScolarite modifierFrais(Long id, FraisScolarite fraisDetails);
    Optional<FraisScolarite> trouverParId(Long id);
    List<FraisScolarite> listerParClasse(Long classeId);
    void supprimerFrais(Long id);
}


