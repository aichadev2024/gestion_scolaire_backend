package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.CreateEtablissementWithAdminRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.EtablissementResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.ModifierEtablissementRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.StatutEtablissement;

import java.util.List;

public interface EtablissementService {
    EtablissementResponse creerEtablissementAvecAdmin(CreateEtablissementWithAdminRequest request);
    List<EtablissementResponse> listerTous();
    EtablissementResponse obtenirParId(Long id);
    EtablissementResponse modifierStatut(Long id, StatutEtablissement statut);

    /** Coordonnées (nom, contact, adresse) — utile notamment pour compléter le reçu d'abonnement après coup. */
    EtablissementResponse modifierInfos(Long id, ModifierEtablissementRequest request);

    /**
     * Renouvelle l'abonnement : la durée payée s'ajoute à la date d'expiration
     * actuelle si elle n'est pas encore dépassée (pas de temps perdu en
     * renouvelant en avance), sinon repart d'aujourd'hui. Réactive
     * automatiquement l'établissement s'il était suspendu pour expiration.
     */
    EtablissementResponse renouvelerAbonnement(Long id, String planTarifaire, int dureeMois);
}


