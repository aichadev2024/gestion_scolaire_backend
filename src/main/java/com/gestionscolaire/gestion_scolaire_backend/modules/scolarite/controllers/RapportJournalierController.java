package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.RapportJournalierRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.RapportJournalier;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.RapportJournalierService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rapports-journaliers")
public class RapportJournalierController {

    private final RapportJournalierService rapportJournalierService;

    public RapportJournalierController(RapportJournalierService rapportJournalierService) {
        this.rapportJournalierService = rapportJournalierService;
    }

    /** Crée le rapport du jour pour cet élève, ou met à jour celui déjà existant. */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SECRETAIRE', 'ENSEIGNANT')")
    public ResponseEntity<RapportJournalier> enregistrer(@Valid @RequestBody RapportJournalierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rapportJournalierService.enregistrer(request));
    }

    @GetMapping("/eleve/{eleveId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SECRETAIRE', 'ENSEIGNANT', 'ELEVE', 'PARENT')")
    public ResponseEntity<List<RapportJournalier>> listerParEleve(@PathVariable Long eleveId) {
        return ResponseEntity.ok(rapportJournalierService.listerParEleve(eleveId));
    }

    /** Vue par groupe/classe pour une date donnée — permet à une monitrice de remplir tous les enfants du groupe. */
    @GetMapping("/classe/{classeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SECRETAIRE', 'ENSEIGNANT')")
    public ResponseEntity<List<RapportJournalier>> listerParClasseEtDate(
            @PathVariable Long classeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(rapportJournalierService.listerParClasseEtDate(classeId, date));
    }
}
