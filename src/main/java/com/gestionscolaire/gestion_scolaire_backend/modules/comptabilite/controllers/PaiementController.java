package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto.PaiementRequest;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.Paiement;
import com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.RecuPdfService;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.services.PaiementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/paiements")
public class PaiementController {

    private final PaiementService paiementService;
    private final RecuPdfService recuPdfService;

    public PaiementController(PaiementService paiementService, RecuPdfService recuPdfService) {
        this.paiementService = paiementService;
        this.recuPdfService = recuPdfService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPTABLE', 'PARENT')")
    public ResponseEntity<Paiement> enregistrer(@Valid @RequestBody PaiementRequest request) {
        Paiement paiement = Paiement.builder()
                .montantPaye(request.getMontantPaye())
                .modePaiement(request.getModePaiement())
                .referenceTransaction(request.getReferenceTransaction())
                .build();
        Paiement saved = paiementService.enregistrerPaiement(
                paiement, request.getEleveId(), request.getFraisId(), SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/eleve/{eleveId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPTABLE', 'DIRECTEUR', 'PARENT')")
    public ResponseEntity<List<Paiement>> listerParEleve(@PathVariable Long eleveId) {
        return ResponseEntity.ok(paiementService.listerPaiementsEleve(eleveId));
    }

    @GetMapping("/eleve/{eleveId}/solde")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPTABLE', 'DIRECTEUR', 'PARENT')")
    public ResponseEntity<Map<String, Double>> calculerSolde(@PathVariable Long eleveId) {
        Double solde = paiementService.calculerSoldeRestantEleve(eleveId);
        return ResponseEntity.ok(Map.of("soldeRestant", solde));
    }

    @GetMapping("/recu/{numeroRecu}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPTABLE', 'PARENT')")
    public ResponseEntity<Paiement> trouverParRecu(@PathVariable String numeroRecu) {
        return ResponseEntity.ok(paiementService.trouverParNumeroRecu(numeroRecu)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable")));
    }

    @GetMapping("/recu/{numeroRecu}/pdf")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPTABLE', 'PARENT')")
    public ResponseEntity<byte[]> telechargerRecuPdf(@PathVariable String numeroRecu) {
        byte[] pdf = recuPdfService.genererRecuPdf(numeroRecu);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recu-" + numeroRecu + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}


