package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.controllers;

import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.StatistiquesEtablissementResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.StatistiquesFinancesResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services.StatistiquesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistiques")
public class StatistiquesController {

    private final StatistiquesService statistiquesService;

    public StatistiquesController(StatistiquesService statistiquesService) {
        this.statistiquesService = statistiquesService;
    }

    /** Vue d'ensemble (effectifs + finances) de son propre établissement — directeur ou promoteur. */
    @GetMapping("/etablissement")
    @PreAuthorize("hasAnyRole('DIRECTEUR', 'PROMOTEUR')")
    public ResponseEntity<StatistiquesEtablissementResponse> obtenirStatistiques() {
        return ResponseEntity.ok(statistiquesService.obtenirPourEtablissementCourant());
    }

    /** Détail des finances : mois/année en cours, courbe des 12 derniers mois, chaque paiement (élève, frais). */
    @GetMapping("/finances")
    @PreAuthorize("hasAnyRole('DIRECTEUR', 'PROMOTEUR')")
    public ResponseEntity<StatistiquesFinancesResponse> obtenirFinances() {
        return ResponseEntity.ok(statistiquesService.obtenirFinancesDetaillees());
    }
}
