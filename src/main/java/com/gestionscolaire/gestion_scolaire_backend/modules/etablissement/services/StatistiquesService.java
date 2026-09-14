package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.StatistiquesEtablissementResponse;

public interface StatistiquesService {
    /** Statistiques de l'établissement de l'utilisateur courant (directeur ou promoteur). */
    StatistiquesEtablissementResponse obtenirPourEtablissementCourant();
}
