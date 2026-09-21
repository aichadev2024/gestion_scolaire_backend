package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.EffectiviteCoursResponse;
import com.gestionscolaire.gestion_scolaire_backend.core.dto.MonCoursResponse;
import com.gestionscolaire.gestion_scolaire_backend.core.dto.SeanceCoursRequest;
import com.gestionscolaire.gestion_scolaire_backend.core.dto.SeanceCoursResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.SeanceCoursService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cahier-texte")
public class SeanceCoursController {

    private final SeanceCoursService seanceCoursService;

    public SeanceCoursController(SeanceCoursService seanceCoursService) {
        this.seanceCoursService = seanceCoursService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTEUR', 'ENSEIGNANT')")
    public ResponseEntity<SeanceCoursResponse> creer(@Valid @RequestBody SeanceCoursRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seanceCoursService.creer(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTEUR', 'ENSEIGNANT')")
    public ResponseEntity<SeanceCoursResponse> modifier(@PathVariable Long id, @Valid @RequestBody SeanceCoursRequest request) {
        return ResponseEntity.ok(seanceCoursService.modifier(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTEUR', 'ENSEIGNANT')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        seanceCoursService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/classe/{classeId}")
    @PreAuthorize("hasAnyRole('DIRECTEUR', 'SECRETAIRE', 'SURVEILLANT_GENERAL', 'ENSEIGNANT')")
    public ResponseEntity<List<SeanceCoursResponse>> lister(
            @PathVariable Long classeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin
    ) {
        return ResponseEntity.ok(seanceCoursService.lister(classeId, debut, fin));
    }

    /** Cahier de texte et devoirs de la classe d'un élève, pour ses parents et pour lui. */
    @GetMapping("/eleve/{eleveId}")
    @PreAuthorize("hasAnyRole('PARENT', 'ELEVE', 'DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<List<SeanceCoursResponse>> pourEleve(
            @PathVariable Long eleveId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin
    ) {
        return ResponseEntity.ok(seanceCoursService.listerPourEleve(eleveId, debut, fin));
    }

    /** Les matières/classes de l'enseignant connecté (pour choisir où saisir sa séance). */
    @GetMapping("/mes-cours")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<List<MonCoursResponse>> mesCours() {
        return ResponseEntity.ok(seanceCoursService.mesCours());
    }

    @GetMapping("/effectivite")
    @PreAuthorize("hasAnyRole('DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<List<EffectiviteCoursResponse>> effectivite(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin
    ) {
        return ResponseEntity.ok(seanceCoursService.effectivite(debut, fin));
    }
}
