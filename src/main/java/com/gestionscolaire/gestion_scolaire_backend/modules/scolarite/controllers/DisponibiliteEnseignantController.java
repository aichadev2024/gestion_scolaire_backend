package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.DisponibiliteRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.DisponibiliteEnseignant;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.DisponibiliteEnseignantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Disponibilités/indisponibilités déclarées par l'enseignant — consultées par la
 * direction pour construire l'emploi du temps (voir V27__disponibilites_enseignant.sql).
 * L'enseignant ne crée jamais lui-même de créneau de cours (EmploiDuTempsController).
 */
@RestController
@RequestMapping("/api/disponibilites")
public class DisponibiliteEnseignantController {

    private final DisponibiliteEnseignantService disponibiliteService;

    public DisponibiliteEnseignantController(DisponibiliteEnseignantService disponibiliteService) {
        this.disponibiliteService = disponibiliteService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<DisponibiliteEnseignant> creer(@Valid @RequestBody DisponibiliteRequest request) {
        DisponibiliteEnseignant dispo = DisponibiliteEnseignant.builder()
                .jourSemaine(request.getJourSemaine())
                .heureDebut(request.getHeureDebut())
                .heureFin(request.getHeureFin())
                .type(request.getType())
                .commentaire(request.getCommentaire())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(disponibiliteService.creerPourMoi(dispo));
    }

    @GetMapping("/moi")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<List<DisponibiliteEnseignant>> listerPourMoi() {
        return ResponseEntity.ok(disponibiliteService.listerPourMoi());
    }

    @GetMapping("/enseignant/{enseignantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<List<DisponibiliteEnseignant>> listerParEnseignant(@PathVariable Long enseignantId) {
        return ResponseEntity.ok(disponibiliteService.listerParEnseignant(enseignantId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<Map<String, String>> supprimer(@PathVariable Long id) {
        disponibiliteService.supprimer(id);
        return ResponseEntity.ok(Map.of("message", "Disponibilité supprimée"));
    }
}
