package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.FraisScolariteRequest;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.FraisScolarite;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.FraisScolariteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/frais-scolarite")
public class FraisScolariteController {

    private final FraisScolariteService fraisScolariteService;

    public FraisScolariteController(FraisScolariteService fraisScolariteService) {
        this.fraisScolariteService = fraisScolariteService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPTABLE')")
    public ResponseEntity<FraisScolarite> creer(@Valid @RequestBody FraisScolariteRequest request) {
        FraisScolarite frais = FraisScolarite.builder()
                .titre(request.getTitre())
                .montant(request.getMontant())
                .dateEcheance(request.getDateEcheance())
                .build();
        FraisScolarite saved = fraisScolariteService.creerFrais(frais, request.getClasseId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/classe/{classeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPTABLE', 'DIRECTEUR', 'PARENT')")
    public ResponseEntity<List<FraisScolarite>> listerParClasse(@PathVariable Long classeId) {
        return ResponseEntity.ok(fraisScolariteService.listerParClasse(classeId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPTABLE', 'DIRECTEUR', 'PARENT')")
    public ResponseEntity<FraisScolarite> trouverParId(@PathVariable Long id) {
        return ResponseEntity.ok(fraisScolariteService.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Frais de scolarité introuvables")));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPTABLE')")
    public ResponseEntity<FraisScolarite> modifier(@PathVariable Long id, @Valid @RequestBody FraisScolariteRequest request) {
        FraisScolarite details = FraisScolarite.builder()
                .titre(request.getTitre())
                .montant(request.getMontant())
                .dateEcheance(request.getDateEcheance())
                .build();
        return ResponseEntity.ok(fraisScolariteService.modifierFrais(id, details));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPTABLE')")
    public ResponseEntity<Map<String, String>> supprimer(@PathVariable Long id) {
        fraisScolariteService.supprimerFrais(id);
        return ResponseEntity.ok(Map.of("message", "Frais supprimé"));
    }
}


