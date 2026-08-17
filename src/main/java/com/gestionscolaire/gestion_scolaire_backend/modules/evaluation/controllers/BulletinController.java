package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.controllers;

import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto.BulletinResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.services.BulletinService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bulletins")
public class BulletinController {

    private final BulletinService bulletinService;

    public BulletinController(BulletinService bulletinService) {
        this.bulletinService = bulletinService;
    }

    @PostMapping("/generer")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DIRECTEUR', 'SECRETAIRE')")
    public ResponseEntity<BulletinResponse> genererBulletin(
            @RequestParam Long eleveId,
            @RequestParam String periode,
            @RequestParam String anneeScolaire
    ) {
        return ResponseEntity.ok(bulletinService.genererBulletin(eleveId, periode, anneeScolaire));
    }

    @GetMapping("/eleve/{eleveId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DIRECTEUR', 'SECRETAIRE', 'ENSEIGNANT', 'PARENT', 'ELEVE')")
    public ResponseEntity<BulletinResponse> getBulletinDetails(
            @PathVariable Long eleveId,
            @RequestParam String periode,
            @RequestParam String anneeScolaire
    ) {
        return ResponseEntity.ok(bulletinService.getBulletinDetails(eleveId, periode, anneeScolaire));
    }

    @PostMapping("/{id}/verrouiller")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DIRECTEUR')")
    public ResponseEntity<BulletinResponse> verrouillerBulletin(@PathVariable Long id) {
        return ResponseEntity.ok(bulletinService.verrouillerBulletin(id));
    }
}


