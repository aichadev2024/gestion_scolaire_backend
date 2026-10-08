package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.controllers;

import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto.RapportTrimestrielRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto.RapportTrimestrielResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.services.RapportTrimestrielService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Rapport de période pour la direction : effectifs, présences, résultats, discipline, bilan écrit. */
@RestController
@RequestMapping("/api/rapports-trimestriels")
public class RapportTrimestrielController {

    private final RapportTrimestrielService service;

    public RapportTrimestrielController(RapportTrimestrielService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<RapportTrimestrielResponse> generer(
            @RequestParam(required = false) Long classeId,
            @RequestParam String periode,
            @RequestParam String anneeScolaire,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(service.generer(classeId, periode, anneeScolaire, dateDebut, dateFin));
    }

    @PutMapping
    @PreAuthorize("hasRole('DIRECTEUR')")
    public ResponseEntity<Void> enregistrer(@Valid @RequestBody RapportTrimestrielRequest request) {
        service.enregistrer(request);
        return ResponseEntity.noContent().build();
    }
}
