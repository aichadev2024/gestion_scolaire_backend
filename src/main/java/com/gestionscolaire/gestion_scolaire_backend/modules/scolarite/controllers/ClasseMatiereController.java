package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.ClasseMatiereRequest;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.ClasseMatiere;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.ClasseMatiereService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/classes-matieres")
public class ClasseMatiereController {

    private final ClasseMatiereService classeMatiereService;

    public ClasseMatiereController(ClasseMatiereService classeMatiereService) {
        this.classeMatiereService = classeMatiereService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SECRETAIRE')")
    public ResponseEntity<ClasseMatiere> assigner(@Valid @RequestBody ClasseMatiereRequest request) {
        ClasseMatiere saved = classeMatiereService.assigner(
                request.getClasseId(),
                request.getMatiereId(),
                request.getEnseignantId(),
                request.getCoefficient()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/classe/{classeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SECRETAIRE', 'DIRECTEUR', 'ENSEIGNANT', 'ELEVE', 'PARENT')")
    public ResponseEntity<List<ClasseMatiere>> listerParClasse(@PathVariable Long classeId) {
        return ResponseEntity.ok(classeMatiereService.listerParClasse(classeId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SECRETAIRE', 'DIRECTEUR', 'ENSEIGNANT')")
    public ResponseEntity<ClasseMatiere> trouverParId(@PathVariable Long id) {
        return ResponseEntity.ok(classeMatiereService.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignation classe-matière introuvable")));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SECRETAIRE')")
    public ResponseEntity<ClasseMatiere> modifier(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        Long enseignantId = body.get("enseignantId") != null ? Long.valueOf(body.get("enseignantId").toString()) : null;
        Double coefficient = body.get("coefficient") != null ? Double.valueOf(body.get("coefficient").toString()) : null;
        return ResponseEntity.ok(classeMatiereService.modifier(id, enseignantId, coefficient));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SECRETAIRE')")
    public ResponseEntity<Map<String, String>> supprimer(@PathVariable Long id) {
        classeMatiereService.supprimer(id);
        return ResponseEntity.ok(Map.of("message", "Assignation supprimée"));
    }
}


