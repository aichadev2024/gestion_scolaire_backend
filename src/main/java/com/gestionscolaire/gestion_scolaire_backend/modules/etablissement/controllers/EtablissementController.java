package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.controllers;

import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.CreateEtablissementWithAdminRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.EtablissementResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.ModifierEtablissementRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.RenouvellementRequest;
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
    private final com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services.RecuEtablissementPdfService recuEtablissementPdfService;

    public EtablissementController(
            EtablissementService etablissementService,
            com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services.RecuEtablissementPdfService recuEtablissementPdfService
    ) {
        this.etablissementService = etablissementService;
        this.recuEtablissementPdfService = recuEtablissementPdfService;
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

    @GetMapping("/{id}/recu-pdf")
    public ResponseEntity<byte[]> telechargerRecuPdf(@PathVariable Long id) {
        byte[] pdfBytes = recuEtablissementPdfService.genererRecuAbonnementPdf(id);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=Recu_Abonnement_Etablissement_" + id + ".pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /** Coordonnées (nom, contact, adresse) — le plan et l'expiration passent par /renouveler, le statut par /statut. */
    @PutMapping("/{id}")
    public ResponseEntity<EtablissementResponse> modifierInfos(
            @PathVariable Long id,
            @Valid @RequestBody ModifierEtablissementRequest request
    ) {
        return ResponseEntity.ok(etablissementService.modifierInfos(id, request));
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

    /** Renouvellement d'abonnement (plan + durée payée) — prolonge l'expiration et réactive si suspendu. */
    @PatchMapping("/{id}/renouveler")
    public ResponseEntity<EtablissementResponse> renouveler(
            @PathVariable Long id,
            @Valid @RequestBody RenouvellementRequest request
    ) {
        return ResponseEntity.ok(etablissementService.renouvelerAbonnement(
                id, request.getPlanTarifaire(), request.getDureeMois()));
    }
}


