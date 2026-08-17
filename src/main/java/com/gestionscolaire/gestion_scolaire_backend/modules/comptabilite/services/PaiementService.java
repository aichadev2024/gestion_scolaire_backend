package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.Paiement;

import java.util.List;
import java.util.Optional;

public interface PaiementService {
    Paiement enregistrerPaiement(Paiement paiement, Long eleveId, Long fraisId, Long userReceptionnaireId);
    List<Paiement> listerPaiementsEleve(Long eleveId);
    Optional<Paiement> trouverParNumeroRecu(String numeroRecu);
    Double calculerSoldeRestantEleve(Long eleveId);
}


