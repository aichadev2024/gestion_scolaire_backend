package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.DtoMapper;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.EleveImportRapport;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.EleveInscriptionRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.EleveResponse;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Parent;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.EleveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/eleves")
public class EleveController {

    private final EleveService eleveService;
    private final DtoMapper dtoMapper;

    public EleveController(EleveService eleveService, DtoMapper dtoMapper) {
        this.eleveService = eleveService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<EleveResponse> inscrire(@Valid @RequestBody EleveInscriptionRequest request) {
        Eleve eleve = Eleve.builder().build();
        Profil profil = dtoMapper.toProfil(request.getProfil());
        String motDePasseInitial = com.gestionscolaire.gestion_scolaire_backend.core.security.PasswordGenerator.generer();
        Eleve saved = eleveService.inscrireEleve(eleve, profil, request.getParentId(), request.getClasseId(), motDePasseInitial);
        EleveResponse response = dtoMapper.toEleveResponse(saved);
        response.setMotDePasseInitial(saved.getMotDePasseInitial());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EleveResponse>> listerTous() {
        return ResponseEntity.ok(eleveService.listerTous().stream().map(dtoMapper::toEleveResponse).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EleveResponse> trouverParId(@PathVariable Long id) {
        Eleve eleve = eleveService.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable"));
        return ResponseEntity.ok(dtoMapper.toEleveResponse(eleve));
    }

    @GetMapping("/classe/{classeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EleveResponse>> listerParClasse(@PathVariable Long classeId) {
        return ResponseEntity.ok(eleveService.listerElevesParClasse(classeId).stream().map(dtoMapper::toEleveResponse).toList());
    }

    @GetMapping("/parent/{parentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EleveResponse>> listerParParent(@PathVariable Long parentId) {
        return ResponseEntity.ok(eleveService.listerElevesParParent(parentId).stream().map(dtoMapper::toEleveResponse).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<EleveResponse> modifier(@PathVariable Long id, @Valid @RequestBody EleveInscriptionRequest request) {
        Eleve eleveDetails = Eleve.builder().build();
        if (request.getClasseId() != null) {
            eleveDetails.setClasse(com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe.builder().id(request.getClasseId()).build());
        }
        if (request.getParentId() != null) {
            eleveDetails.setParent(Parent.builder().id(request.getParentId()).build());
        }
        Profil profilDetails = dtoMapper.toProfil(request.getProfil());
        Eleve updated = eleveService.modifierEleve(id, eleveDetails, profilDetails);
        return ResponseEntity.ok(dtoMapper.toEleveResponse(updated));
    }

    @PatchMapping("/{id}/archiver")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<Map<String, String>> archiver(@PathVariable Long id) {
        eleveService.archiverEleve(id);
        return ResponseEntity.ok(Map.of("message", "Élève archivé"));
    }

    /** Statut du dossier d'inscription : VALIDEE, EN_ATTENTE ou ANNULEE. */
    @PatchMapping("/{id}/statut-inscription")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<EleveResponse> modifierStatutInscription(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        Eleve updated = eleveService.modifierStatutInscription(id, body.get("statutInscription"));
        return ResponseEntity.ok(dtoMapper.toEleveResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<Map<String, String>> supprimer(@PathVariable Long id) {
        eleveService.supprimerEleve(id);
        return ResponseEntity.ok(Map.of("message", "Élève supprimé avec succès"));
    }

    /** Import en masse depuis un fichier Excel (.xlsx) — voir /import/modele pour le modèle attendu. */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<EleveImportRapport> importer(
            @RequestParam("fichier") MultipartFile fichier,
            @RequestParam(value = "classeId", required = false) Long classeId) {
        return ResponseEntity.ok(eleveService.importerDepuisExcel(fichier, classeId));
    }

    @GetMapping("/import/modele")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<byte[]> modeleImport() {
        byte[] bytes = eleveService.genererModeleImportExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=modele_import_eleves.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}


