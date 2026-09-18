package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.IncidentDisciplineRequest;
import com.gestionscolaire.gestion_scolaire_backend.core.dto.IncidentDisciplineResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.IncidentDiscipline;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.IncidentDisciplineService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents-discipline")
public class IncidentDisciplineController {

    private final IncidentDisciplineService incidentDisciplineService;

    public IncidentDisciplineController(IncidentDisciplineService incidentDisciplineService) {
        this.incidentDisciplineService = incidentDisciplineService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SURVEILLANT_GENERAL')")
    public ResponseEntity<IncidentDisciplineResponse> enregistrer(@Valid @RequestBody IncidentDisciplineRequest request) {
        IncidentDiscipline incident = IncidentDiscipline.builder()
                .date(request.getDate())
                .heure(request.getHeure())
                .statut(request.getStatut())
                .commentaire(request.getCommentaire())
                .build();
        IncidentDisciplineResponse saved = incidentDisciplineService.enregistrerIncident(
                incident, request.getEleveId(), request.getClasseId(), request.getClasseMatiereId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/eleve/{eleveId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SECRETAIRE', 'SURVEILLANT_GENERAL')")
    public ResponseEntity<List<IncidentDisciplineResponse>> listerParEleve(@PathVariable Long eleveId) {
        return ResponseEntity.ok(incidentDisciplineService.listerParEleve(eleveId));
    }

    @GetMapping("/classe/{classeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SECRETAIRE', 'SURVEILLANT_GENERAL')")
    public ResponseEntity<List<IncidentDisciplineResponse>> listerParClasseEtDate(
            @PathVariable Long classeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(incidentDisciplineService.listerParClasseEtDate(classeId, date));
    }

    @PatchMapping("/{id}/traiter")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SURVEILLANT_GENERAL')")
    public ResponseEntity<IncidentDisciplineResponse> marquerTraite(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(incidentDisciplineService.marquerTraite(id, body.get("notesTraitement")));
    }
}
