package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.EmploiDuTempsRequest;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.EmploiDuTemps;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.EmploiDuTempsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emplois-du-temps")
public class EmploiDuTempsController {

    private final EmploiDuTempsService emploiDuTempsService;

    public EmploiDuTempsController(EmploiDuTempsService emploiDuTempsService) {
        this.emploiDuTempsService = emploiDuTempsService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SECRETAIRE')")
    public ResponseEntity<EmploiDuTemps> creer(@Valid @RequestBody EmploiDuTempsRequest request) {
        com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe classeObj = null;
        if (request.getClasseId() != null) {
            classeObj = com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe.builder().id(request.getClasseId()).build();
        }
        EmploiDuTemps creneau = EmploiDuTemps.builder()
                .jourSemaine(request.getJourSemaine())
                .heureDebut(request.getHeureDebut())
                .heureFin(request.getHeureFin())
                .salle(request.getSalle())
                .typeCreneau(request.getTypeCreneau() != null ? request.getTypeCreneau() : "COURS")
                .libellePause(request.getLibellePause())
                .classe(classeObj)
                .build();
        EmploiDuTemps saved = emploiDuTempsService.creerCreneau(creneau, request.getClasseMatiereId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/classe/{classeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SECRETAIRE', 'DIRECTEUR', 'ENSEIGNANT', 'ELEVE', 'PARENT')")
    public ResponseEntity<List<EmploiDuTemps>> listerParClasse(@PathVariable Long classeId) {
        return ResponseEntity.ok(emploiDuTempsService.listerParClasse(classeId));
    }

    @GetMapping("/enseignant/{enseignantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SECRETAIRE', 'DIRECTEUR', 'ENSEIGNANT')")
    public ResponseEntity<List<EmploiDuTemps>> listerParEnseignant(@PathVariable Long enseignantId) {
        return ResponseEntity.ok(emploiDuTempsService.listerParEnseignant(enseignantId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SECRETAIRE')")
    public ResponseEntity<EmploiDuTemps> modifier(@PathVariable Long id, @Valid @RequestBody EmploiDuTempsRequest request) {
        com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe classeObj = null;
        if (request.getClasseId() != null) {
            classeObj = com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe.builder().id(request.getClasseId()).build();
        }
        EmploiDuTemps details = EmploiDuTemps.builder()
                .jourSemaine(request.getJourSemaine())
                .heureDebut(request.getHeureDebut())
                .heureFin(request.getHeureFin())
                .salle(request.getSalle())
                .typeCreneau(request.getTypeCreneau() != null ? request.getTypeCreneau() : "COURS")
                .libellePause(request.getLibellePause())
                .classe(classeObj)
                .build();
        return ResponseEntity.ok(emploiDuTempsService.modifierCreneau(id, details, request.getClasseMatiereId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SECRETAIRE')")
    public ResponseEntity<Map<String, String>> supprimer(@PathVariable Long id) {
        emploiDuTempsService.supprimerCreneau(id);
        return ResponseEntity.ok(Map.of("message", "Créneau supprimé"));
    }
}


