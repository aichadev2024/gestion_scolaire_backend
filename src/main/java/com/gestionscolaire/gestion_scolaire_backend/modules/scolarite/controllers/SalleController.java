package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.SalleRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Salle;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.SalleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Catalogue optionnel des salles nommées d'un établissement (voir V26__salles.sql). */
@RestController
@RequestMapping("/api/salles")
public class SalleController {

    private final SalleService salleService;

    public SalleController(SalleService salleService) {
        this.salleService = salleService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<Salle> creer(@Valid @RequestBody SalleRequest request) {
        Salle salle = Salle.builder().nom(request.getNom()).capacite(request.getCapacite()).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(salleService.creerSalle(salle));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Salle>> listerToutes() {
        return ResponseEntity.ok(salleService.listerToutes());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<Salle> modifier(@PathVariable Long id, @Valid @RequestBody SalleRequest request) {
        Salle details = Salle.builder().nom(request.getNom()).capacite(request.getCapacite()).build();
        return ResponseEntity.ok(salleService.modifierSalle(id, details));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<Map<String, String>> supprimer(@PathVariable Long id) {
        salleService.supprimerSalle(id);
        return ResponseEntity.ok(Map.of("message", "Salle supprimée"));
    }
}
