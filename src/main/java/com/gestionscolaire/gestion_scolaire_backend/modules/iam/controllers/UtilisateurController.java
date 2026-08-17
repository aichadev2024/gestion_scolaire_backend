package com.gestionscolaire.gestion_scolaire_backend.modules.iam.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.DtoMapper;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto.RegisterUtilisateurRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto.UtilisateurResponse;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.ProfilRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.services.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;
    private final ProfilRepository profilRepository;
    private final DtoMapper dtoMapper;

    public UtilisateurController(UtilisateurService utilisateurService, ProfilRepository profilRepository, DtoMapper dtoMapper) {
        this.utilisateurService = utilisateurService;
        this.profilRepository = profilRepository;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<UtilisateurResponse> inscrire(@Valid @RequestBody RegisterUtilisateurRequest request) {
        Utilisateur utilisateur = Utilisateur.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .motDePasse(request.getMotDePasse())
                .build();
        Profil profil = dtoMapper.toProfil(request.getProfil());
        Utilisateur saved = utilisateurService.inscrire(utilisateur, profil, request.getRole());
        Profil savedProfil = profilRepository.findByUtilisateurId(saved.getId()).orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toUtilisateurResponse(saved, savedProfil));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DIRECTEUR')")
    public ResponseEntity<List<UtilisateurResponse>> listerTous() {
        List<UtilisateurResponse> response = utilisateurService.listerTous().stream()
                .map(u -> dtoMapper.toUtilisateurResponse(u, profilRepository.findByUtilisateurId(u.getId()).orElse(null)))
                .toList();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, String>> modifierStatut(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean estActif = body.get("estActif");
        if (estActif == null) {
            throw new ResourceNotFoundException("Le champ estActif est obligatoire");
        }
        utilisateurService.modifierStatut(id, estActif);
        return ResponseEntity.ok(Map.of("message", "Statut mis à jour"));
    }
}


