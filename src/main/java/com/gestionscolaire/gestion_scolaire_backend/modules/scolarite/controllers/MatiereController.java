package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.MatiereRequest;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Matiere;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.MatiereService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matieres")
public class MatiereController {

    private final MatiereService matiereService;

    public MatiereController(MatiereService matiereService) {
        this.matiereService = matiereService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SECRETAIRE')")
    public ResponseEntity<Matiere> creer(@Valid @RequestBody MatiereRequest request) {
        Matiere matiere = Matiere.builder().nom(request.getNom()).code(request.getCode()).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(matiereService.creerMatiere(matiere));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Matiere>> listerToutes() {
        return ResponseEntity.ok(matiereService.listerToutes());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Matiere> trouverParId(@PathVariable Long id) {
        return ResponseEntity.ok(matiereService.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matière introuvable")));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SECRETAIRE')")
    public ResponseEntity<Matiere> modifier(@PathVariable Long id, @Valid @RequestBody MatiereRequest request) {
        Matiere details = Matiere.builder().nom(request.getNom()).code(request.getCode()).build();
        return ResponseEntity.ok(matiereService.modifierMatiere(id, details));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, String>> supprimer(@PathVariable Long id) {
        matiereService.supprimerMatiere(id);
        return ResponseEntity.ok(Map.of("message", "Matière supprimée"));
    }
}


