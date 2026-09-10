package com.gestionscolaire.gestion_scolaire_backend.modules.iam.controllers;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.services.ProfilPhotoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/profils")
public class ProfilController {

    private final ProfilPhotoService photoService;

    public ProfilController(ProfilPhotoService photoService) {
        this.photoService = photoService;
    }

    /** Upload / remplacement de la photo d'un profil. Renvoie l'URL publique. */
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<Map<String, String>> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        String url = photoService.uploadPhoto(id, file);
        return ResponseEntity.ok(Map.of("photoUrl", url));
    }

    /** Suppression de la photo d'un profil. */
    @DeleteMapping("/{id}/photo")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<Void> removePhoto(@PathVariable Long id) {
        photoService.removePhoto(id);
        return ResponseEntity.noContent().build();
    }
}
