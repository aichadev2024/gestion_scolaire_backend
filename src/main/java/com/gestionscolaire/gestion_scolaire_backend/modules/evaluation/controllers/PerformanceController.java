package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.DecisionPassageRequest;
import com.gestionscolaire.gestion_scolaire_backend.core.dto.PerformanceClasseResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.services.PerformanceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    private final PerformanceService performanceService;

    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @GetMapping("/classe/{classeId}")
    @PreAuthorize("hasAnyRole('DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<PerformanceClasseResponse> performanceClasse(
            @PathVariable Long classeId,
            @RequestParam(required = false) String periode,
            @RequestParam(required = false) Double seuilPassage,
            @RequestParam(required = false) Double seuilRedoublement
    ) {
        return ResponseEntity.ok(performanceService.performanceClasse(classeId, periode, seuilPassage, seuilRedoublement));
    }

    @PutMapping("/decisions")
    @PreAuthorize("hasRole('DIRECTEUR')")
    public ResponseEntity<Map<String, String>> enregistrerDecision(@Valid @RequestBody DecisionPassageRequest request) {
        performanceService.enregistrerDecision(request);
        return ResponseEntity.ok(Map.of("message", "Décision enregistrée."));
    }
}
