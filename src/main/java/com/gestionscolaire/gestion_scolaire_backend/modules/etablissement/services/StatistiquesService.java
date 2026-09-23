package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.StatistiquesEtablissementResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.StatistiquesFinancesResponse;

public interface StatistiquesService {
    /** Statistiques de l'établissement de l'utilisateur courant (directeur ou promoteur). */
    StatistiquesEtablissementResponse obtenirPourEtablissementCourant();

    /** Détail des finances : mois/année en cours, courbe des 12 derniers mois, paiements un par un. */
    StatistiquesFinancesResponse obtenirFinancesDetaillees();
}
