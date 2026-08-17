package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.NotificationRequest;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Notification;
import com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ENSEIGNANT', 'PARENT', 'SECRETAIRE', 'DIRECTEUR')")
    public ResponseEntity<Notification> envoyer(@Valid @RequestBody NotificationRequest request) {
        Notification notification = Notification.builder()
                .titre(request.getTitre())
                .contenu(request.getContenu())
                .build();
        Long expediteurId = request.getExpediteurId() != null ? request.getExpediteurId() : SecurityUtils.getCurrentUserId();
        Notification saved = notificationService.envoyerNotification(notification, expediteurId, request.getDestinataireId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/destinataire/{destinataireId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENSEIGNANT', 'PARENT', 'ELEVE', 'SECRETAIRE', 'DIRECTEUR', 'COMPTABLE')")
    public ResponseEntity<List<Notification>> listerPourDestinataire(@PathVariable Long destinataireId) {
        return ResponseEntity.ok(notificationService.listerPourDestinataire(destinataireId));
    }

    @GetMapping("/destinataire/{destinataireId}/non-lues")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENSEIGNANT', 'PARENT', 'ELEVE', 'SECRETAIRE', 'DIRECTEUR', 'COMPTABLE')")
    public ResponseEntity<List<Notification>> listerNonLues(@PathVariable Long destinataireId) {
        return ResponseEntity.ok(notificationService.listerNonLues(destinataireId));
    }

    @PatchMapping("/{id}/lue")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENSEIGNANT', 'PARENT', 'ELEVE', 'SECRETAIRE', 'DIRECTEUR', 'COMPTABLE')")
    public ResponseEntity<Map<String, String>> marquerCommeLue(@PathVariable Long id) {
        notificationService.marquerCommeLue(id);
        return ResponseEntity.ok(Map.of("message", "Notification marquée comme lue"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENSEIGNANT', 'PARENT', 'ELEVE', 'SECRETAIRE', 'DIRECTEUR', 'COMPTABLE')")
    public ResponseEntity<Notification> trouverParId(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable")));
    }
}


