package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.DtoMapper;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.EnseignantRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.EnseignantResponse;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Enseignant;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.EnseignantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enseignants")
public class EnseignantController {

    private final EnseignantService enseignantService;
    private final DtoMapper dtoMapper;

    public EnseignantController(EnseignantService enseignantService, DtoMapper dtoMapper) {
        this.enseignantService = enseignantService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SECRETAIRE')")
    public ResponseEntity<EnseignantResponse> creer(@Valid @RequestBody EnseignantRequest request) {
        Enseignant enseignant = Enseignant.builder().biographie(request.getBiographie()).build();
        Profil profil = dtoMapper.toProfil(request.getProfil());
        Enseignant saved = enseignantService.creerEnseignant(enseignant, profil);
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toEnseignantResponse(saved));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EnseignantResponse>> listerTous() {
        return ResponseEntity.ok(enseignantService.listerTous().stream().map(dtoMapper::toEnseignantResponse).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EnseignantResponse> trouverParId(@PathVariable Long id) {
        Enseignant enseignant = enseignantService.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enseignant introuvable"));
        return ResponseEntity.ok(dtoMapper.toEnseignantResponse(enseignant));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SECRETAIRE')")
    public ResponseEntity<EnseignantResponse> modifier(@PathVariable Long id, @Valid @RequestBody EnseignantRequest request) {
        Enseignant details = Enseignant.builder().biographie(request.getBiographie()).build();
        Profil profilDetails = dtoMapper.toProfil(request.getProfil());
        Enseignant updated = enseignantService.modifierEnseignant(id, details, profilDetails);
        return ResponseEntity.ok(dtoMapper.toEnseignantResponse(updated));
    }
}


