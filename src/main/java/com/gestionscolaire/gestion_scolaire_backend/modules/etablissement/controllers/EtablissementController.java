package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.controllers;

import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.CreateEtablissementWithAdminRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.EtablissementResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.StatutEtablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services.EtablissementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/super-admin/etablissements")
@PreAuthorize("hasAnyRole('SUPER_ADMIN')")
public class EtablissementController {

    private final EtablissementService etablissementService;

    public EtablissementController(EtablissementService etablissementService) {
        this.etablissementService = etablissementService;
    }

    @PostMapping
    public ResponseEntity<EtablissementResponse> creer(@Valid @RequestBody CreateEtablissementWithAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(etablissementService.creerEtablissementAvecAdmin(request));
    }

    @GetMapping
    public ResponseEntity<List<EtablissementResponse>> listerTous() {
        return ResponseEntity.ok(etablissementService.listerTous());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EtablissementResponse> obtenirParId(@PathVariable Long id) {
        return ResponseEntity.ok(etablissementService.obtenirParId(id));
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<EtablissementResponse> modifierStatut(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String statutStr = body.get("statut");
        StatutEtablissement statut = StatutEtablissement.valueOf(statutStr);
        return ResponseEntity.ok(etablissementService.modifierStatut(id, statut));
    }
}


