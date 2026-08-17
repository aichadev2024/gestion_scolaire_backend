package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.PresenceRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Presence;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.PresenceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/presences")
public class PresenceController {

    private final PresenceService presenceService;
    private final com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.PresenceEnseignantService presenceEnseignantService;

    public PresenceController(
            PresenceService presenceService,
            com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.PresenceEnseignantService presenceEnseignantService
    ) {
        this.presenceService = presenceService;
        this.presenceEnseignantService = presenceEnseignantService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ENSEIGNANT')")
    public ResponseEntity<Presence> enregistrer(@Valid @RequestBody PresenceRequest request) {
        Presence presence = Presence.builder()
                .date(request.getDate())
                .statut(request.getStatut())
                .estJustifie(request.getEstJustifie())
                .notesJustification(request.getNotesJustification())
                .build();
        Presence saved = presenceService.enregistrerPresence(presence, request.getEleveId(), request.getClasseMatiereId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/eleve/{eleveId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ENSEIGNANT', 'DIRECTEUR', 'ELEVE', 'PARENT')")
    public ResponseEntity<List<Presence>> listerParEleve(@PathVariable Long eleveId) {
        return ResponseEntity.ok(presenceService.listerPresencesEleve(eleveId));
    }

    @GetMapping("/classe-matiere/{classeMatiereId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'ENSEIGNANT', 'DIRECTEUR')")
    public ResponseEntity<List<Presence>> listerParClasseMatiereEtDate(
            @PathVariable Long classeMatiereId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(presenceService.listerPresencesParClasseMatiereEtDate(classeMatiereId, date));
    }

    // ── Présences des Enseignants ──
    @PostMapping("/enseignants")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DIRECTEUR')")
    public ResponseEntity<com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.PresenceEnseignant> enregistrerPresenceEnseignant(
            @Valid @RequestBody com.gestionscolaire.gestion_scolaire_backend.core.dto.PresenceEnseignantRequest request
    ) {
        com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.PresenceEnseignant presence = com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.PresenceEnseignant.builder()
                .date(request.getDate())
                .statut(request.getStatut())
                .heureArrivee(request.getHeureArrivee())
                .heureDepart(request.getHeureDepart())
                .remarques(request.getRemarques())
                .build();
        com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.PresenceEnseignant saved = presenceEnseignantService.enregistrerPresence(presence, request.getEnseignantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/enseignants")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DIRECTEUR')")
    public ResponseEntity<List<com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.PresenceEnseignant>> listerPresencesEnseignants(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(presenceEnseignantService.listerParDate(date));
    }

    @GetMapping("/enseignants/{enseignantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DIRECTEUR', 'ENSEIGNANT')")
    public ResponseEntity<List<com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.PresenceEnseignant>> listerPresencesParEnseignant(
            @PathVariable Long enseignantId
    ) {
        return ResponseEntity.ok(presenceEnseignantService.listerParEnseignant(enseignantId));
    }
}


