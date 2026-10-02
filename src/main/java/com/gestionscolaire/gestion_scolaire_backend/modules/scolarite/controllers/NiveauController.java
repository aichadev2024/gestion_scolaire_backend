package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Niveau;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.NiveauService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/niveaux")
public class NiveauController {

    private final NiveauService niveauService;

    public NiveauController(NiveauService niveauService) {
        this.niveauService = niveauService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR')")
    public ResponseEntity<Niveau> creer(@RequestBody Map<String, @NotBlank String> body) {
        String nom = body.get("nom");
        if (nom == null || nom.isBlank()) {
            throw new BadRequestException("Le champ nom est obligatoire");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(niveauService.creer(nom));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Niveau>> listerTous() {
        return ResponseEntity.ok(niveauService.listerTous().stream().map(this::pourAffichage).toList());
    }

    /**
     * Copie détachée portant le nom propre à l'école de l'utilisateur (ex. « IFTICA » à la place de
     * « Enseignement Professionnel »). Jamais l'entité gérée elle-même : renommer l'entité la
     * modifierait en base pour toutes les écoles. Le super-admin (sans école) voit les noms du catalogue.
     */
    private Niveau pourAffichage(Niveau niveau) {
        com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement etab = null;
        try {
            etab = com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils.getCurrentUser().getUtilisateur().getEtablissement();
        } catch (Exception ignored) {
            // pas de contexte d'authentification exploitable
        }
        return Niveau.builder().id(niveau.getId()).nom(Niveau.libelle(niveau, etab)).build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Niveau> trouverParId(@PathVariable Integer id) {
        return ResponseEntity.ok(pourAffichage(niveauService.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Niveau introuvable"))));
    }
}


