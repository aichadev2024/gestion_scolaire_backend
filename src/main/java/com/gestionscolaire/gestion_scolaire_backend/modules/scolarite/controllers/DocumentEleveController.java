package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.DocumentEleveResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.DocumentEleveService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Pièces du dossier d'inscription d'un élève (acte de naissance, etc.). */
@RestController
@RequestMapping("/api/eleves/{eleveId}/documents")
public class DocumentEleveController {

    private final DocumentEleveService documentEleveService;

    public DocumentEleveController(DocumentEleveService documentEleveService) {
        this.documentEleveService = documentEleveService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<DocumentEleveResponse> ajouter(
            @PathVariable Long eleveId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "libelle", required = false) String libelle) {
        return ResponseEntity.ok(documentEleveService.ajouter(eleveId, type, libelle, file));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<List<DocumentEleveResponse>> lister(@PathVariable Long eleveId) {
        return ResponseEntity.ok(documentEleveService.lister(eleveId));
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<Void> supprimer(@PathVariable Long eleveId, @PathVariable Long documentId) {
        documentEleveService.supprimer(eleveId, documentId);
        return ResponseEntity.noContent().build();
    }
}
