package com.gestionscolaire.gestion_scolaire_backend.modules.iam.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.DtoMapper;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto.RegisterUtilisateurRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto.UtilisateurResponse;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.ProfilRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.services.UtilisateurService;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Niveau;
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
    private final TenantGuard tenantGuard;

    public UtilisateurController(UtilisateurService utilisateurService, ProfilRepository profilRepository, DtoMapper dtoMapper, TenantGuard tenantGuard) {
        this.utilisateurService = utilisateurService;
        this.profilRepository = profilRepository;
        this.dtoMapper = dtoMapper;
        this.tenantGuard = tenantGuard;
    }

    /** Un directeur restreint à un niveau ne peut gérer (créer/éditer) que des comptes de ce
     * même niveau — jamais un compte non restreint, ce qui serait une élévation de privilège. */
    private void validerNiveauCible(Integer niveauCibleId) {
        if (!tenantGuard.niveauRestreint()) return;
        if (!tenantGuard.correspondAuNiveauCourant(niveauCibleId)) {
            throw new BadRequestException("Vous ne pouvez gérer que les comptes de votre propre niveau.");
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR')")
    public ResponseEntity<UtilisateurResponse> inscrire(@Valid @RequestBody RegisterUtilisateurRequest request) {
        validerNiveauCible(request.getNiveauSuperviseId());
        Utilisateur utilisateur = Utilisateur.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .motDePasse(request.getMotDePasse())
                .niveauSupervise(request.getNiveauSuperviseId() != null ? Niveau.builder().id(request.getNiveauSuperviseId()).build() : null)
                .build();
        Profil profil = dtoMapper.toProfil(request.getProfil());
        Utilisateur saved = utilisateurService.inscrire(utilisateur, profil, request.getRole());
        Profil savedProfil = profilRepository.findByUtilisateurId(saved.getId()).orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toUtilisateurResponse(saved, savedProfil));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'PROMOTEUR')")
    public ResponseEntity<List<UtilisateurResponse>> listerTous() {
        List<UtilisateurResponse> response = utilisateurService.listerTous().stream()
                .map(u -> dtoMapper.toUtilisateurResponse(u, profilRepository.findByUtilisateurId(u.getId()).orElse(null)))
                .toList();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR')")
    public ResponseEntity<Map<String, String>> modifierStatut(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean estActif = body.get("estActif");
        if (estActif == null) {
            throw new ResourceNotFoundException("Le champ estActif est obligatoire");
        }
        utilisateurService.modifierStatut(id, estActif);
        return ResponseEntity.ok(Map.of("message", "Statut mis à jour"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR')")
    public ResponseEntity<UtilisateurResponse> modifier(@PathVariable Long id, @RequestBody RegisterUtilisateurRequest request) {
        validerNiveauCible(request.getNiveauSuperviseId());
        Utilisateur details = Utilisateur.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .motDePasse(request.getMotDePasse())
                .build();
        Profil profil = dtoMapper.toProfil(request.getProfil());
        Utilisateur updated = utilisateurService.modifierUtilisateur(id, details, profil, request.getRole(), request.getNiveauSuperviseId());
        Profil updatedProfil = profilRepository.findByUtilisateurId(updated.getId()).orElse(null);
        return ResponseEntity.ok(dtoMapper.toUtilisateurResponse(updated, updatedProfil));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR')")
    public ResponseEntity<Map<String, String>> supprimer(@PathVariable Long id) {
        utilisateurService.supprimerUtilisateur(id);
        return ResponseEntity.ok(Map.of("message", "Compte utilisateur supprimé avec succès"));
    }

    /** Nomme le directeur d'un niveau donné : cet utilisateur devient DIRECTEUR de ce niveau,
     * l'ancien titulaire de CE MÊME niveau (s'il y en a un) redevient Secrétaire. */
    @PatchMapping("/{id}/nommer-directeur")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR')")
    public ResponseEntity<UtilisateurResponse> nommerDirecteur(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer niveauId = body.get("niveauId");
        validerNiveauCible(niveauId);
        Utilisateur updated = utilisateurService.nommerDirecteur(id, niveauId);
        Profil updatedProfil = profilRepository.findByUtilisateurId(updated.getId()).orElse(null);
        return ResponseEntity.ok(dtoMapper.toUtilisateurResponse(updated, updatedProfil));
    }
}


