package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.DtoMapper;
import com.gestionscolaire.gestion_scolaire_backend.core.dto.FraisScolariteRequest;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.FraisScolariteResponse;
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
    private final DtoMapper dtoMapper;

    public FraisScolariteController(FraisScolariteService fraisScolariteService, DtoMapper dtoMapper) {
        this.fraisScolariteService = fraisScolariteService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'COMPTABLE')")
    public ResponseEntity<FraisScolariteResponse> creer(@Valid @RequestBody FraisScolariteRequest request) {
        FraisScolarite frais = FraisScolarite.builder()
                .titre(request.getTitre())
                .montant(request.getMontant())
                .dateEcheance(request.getDateEcheance())
                .build();
        FraisScolarite saved = fraisScolariteService.creerFrais(frais, request.getClasseId());
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toFraisScolariteResponse(saved));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FraisScolariteResponse>> listerTous() {
        return ResponseEntity.ok(fraisScolariteService.listerTous().stream().map(dtoMapper::toFraisScolariteResponse).toList());
    }

    @GetMapping("/classe/{classeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'COMPTABLE', 'PARENT')")
    public ResponseEntity<List<FraisScolariteResponse>> listerParClasse(@PathVariable Long classeId) {
        return ResponseEntity.ok(fraisScolariteService.listerParClasse(classeId).stream().map(dtoMapper::toFraisScolariteResponse).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'COMPTABLE', 'PARENT')")
    public ResponseEntity<FraisScolariteResponse> trouverParId(@PathVariable Long id) {
        FraisScolarite frais = fraisScolariteService.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Frais de scolarité introuvables"));
        return ResponseEntity.ok(dtoMapper.toFraisScolariteResponse(frais));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'COMPTABLE')")
    public ResponseEntity<FraisScolariteResponse> modifier(@PathVariable Long id, @Valid @RequestBody FraisScolariteRequest request) {
        FraisScolarite details = FraisScolarite.builder()
                .titre(request.getTitre())
                .montant(request.getMontant())
                .dateEcheance(request.getDateEcheance())
                .build();
        FraisScolarite updated = fraisScolariteService.modifierFrais(id, details);
        return ResponseEntity.ok(dtoMapper.toFraisScolariteResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'COMPTABLE')")
    public ResponseEntity<Map<String, String>> supprimer(@PathVariable Long id) {
        fraisScolariteService.supprimerFrais(id);
        return ResponseEntity.ok(Map.of("message", "Frais supprimé"));
    }
}


