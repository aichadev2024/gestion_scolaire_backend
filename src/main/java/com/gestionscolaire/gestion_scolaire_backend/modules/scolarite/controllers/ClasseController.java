package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.DtoMapper;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.ClasseRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.ClasseResponse;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.ClasseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
public class ClasseController {

    private final ClasseService classeService;
    private final DtoMapper dtoMapper;

    public ClasseController(ClasseService classeService, DtoMapper dtoMapper) {
        this.classeService = classeService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SECRETAIRE')")
    public ResponseEntity<ClasseResponse> creer(@Valid @RequestBody ClasseRequest request) {
        Classe classe = Classe.builder()
                .nom(request.getNom())
                .anneeScolaire(request.getAnneeScolaire())
                .capaciteMax(request.getCapaciteMax())
                .build();
        Classe saved = classeService.creerClasse(classe, request.getNiveauId(), request.getEnseignantPrincipalId());
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toClasseResponse(saved));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClasseResponse>> listerToutes() {
        return ResponseEntity.ok(classeService.listerToutes().stream().map(dtoMapper::toClasseResponse).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClasseResponse> trouverParId(@PathVariable Long id) {
        Classe classe = classeService.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable"));
        return ResponseEntity.ok(dtoMapper.toClasseResponse(classe));
    }

    @GetMapping("/niveau/{niveauId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClasseResponse>> listerParNiveau(@PathVariable Integer niveauId) {
        return ResponseEntity.ok(classeService.listerParNiveau(niveauId).stream().map(dtoMapper::toClasseResponse).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SECRETAIRE')")
    public ResponseEntity<ClasseResponse> modifier(@PathVariable Long id, @Valid @RequestBody ClasseRequest request) {
        Classe details = Classe.builder()
                .nom(request.getNom())
                .anneeScolaire(request.getAnneeScolaire())
                .capaciteMax(request.getCapaciteMax())
                .build();
        Classe updated = classeService.modifierClasse(id, details, request.getNiveauId(), request.getEnseignantPrincipalId());
        return ResponseEntity.ok(dtoMapper.toClasseResponse(updated));
    }
}


