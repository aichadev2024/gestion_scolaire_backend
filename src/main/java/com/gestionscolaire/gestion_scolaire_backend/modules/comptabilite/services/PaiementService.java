package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto.RetardPaiementResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.Paiement;

import java.util.List;
import java.util.Optional;

public interface PaiementService {
    Paiement enregistrerPaiement(Paiement paiement, Long eleveId, Long fraisId, Long userReceptionnaireId);
    List<Paiement> listerPaiementsEleve(Long eleveId);
    Optional<Paiement> trouverParNumeroRecu(String numeroRecu);
    Double calculerSoldeRestantEleve(Long eleveId);

    /** Frais dus, payés et reste pour un élève, plus l'historique des reçus (avec leur objet). */
    com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto.SituationFinanciereResponse situationEleve(Long eleveId);

    /** Élèves dont au moins une échéance de frais est dépassée d'au moins 1 jour et non couverte. */
    List<RetardPaiementResponse> listerRetardsPaiement();
}


