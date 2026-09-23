package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.SujetDevoirResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.SujetDevoirService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** Sujets de devoir/examen qu'un enseignant transmet à la direction avant de les donner aux élèves. */
@RestController
@RequestMapping("/api/sujets-devoirs")
public class SujetDevoirController {

    private final SujetDevoirService sujetDevoirService;

    public SujetDevoirController(SujetDevoirService sujetDevoirService) {
        this.sujetDevoirService = sujetDevoirService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<SujetDevoirResponse> envoyer(
            @RequestParam("classeMatiereId") Long classeMatiereId,
            @RequestParam("type") String type,
            @RequestParam("titre") String titre,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(sujetDevoirService.envoyer(classeMatiereId, type, titre, description, file));
    }

    @GetMapping("/mes")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<List<SujetDevoirResponse>> mesSujets() {
        return ResponseEntity.ok(sujetDevoirService.mesSujets());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<List<SujetDevoirResponse>> lister(@RequestParam(required = false) String statut) {
        return ResponseEntity.ok(sujetDevoirService.lister(statut));
    }

    @PatchMapping("/{id}/traiter")
    @PreAuthorize("hasRole('DIRECTEUR')")
    public ResponseEntity<SujetDevoirResponse> traiter(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(sujetDevoirService.traiter(id, body.get("statut"), body.get("commentaire")));
    }
}
