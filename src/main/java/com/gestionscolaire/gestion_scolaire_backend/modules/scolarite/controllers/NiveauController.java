package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Niveau;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.NiveauService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/niveaux")
public class NiveauController {

    private final NiveauService niveauService;

    public NiveauController(NiveauService niveauService) {
        this.niveauService = niveauService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Niveau> creer(@RequestBody Map<String, @NotBlank String> body) {
        String nom = body.get("nom");
        if (nom == null || nom.isBlank()) {
            throw new BadRequestException("Le champ nom est obligatoire");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(niveauService.creer(nom));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Niveau>> listerTous() {
        return ResponseEntity.ok(niveauService.listerTous());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Niveau> trouverParId(@PathVariable Integer id) {
        return ResponseEntity.ok(niveauService.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Niveau introuvable")));
    }
}


