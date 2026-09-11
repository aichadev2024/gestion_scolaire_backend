package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.controllers;

import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.ModifierTarifRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.TarifPlanResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services.TarifPlanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin/tarifs")
@PreAuthorize("hasAnyRole('SUPER_ADMIN')")
public class TarifController {

    private final TarifPlanService tarifPlanService;

    public TarifController(TarifPlanService tarifPlanService) {
        this.tarifPlanService = tarifPlanService;
    }

    @GetMapping
    public ResponseEntity<List<TarifPlanResponse>> listerTous() {
        return ResponseEntity.ok(tarifPlanService.listerTous());
    }

    @PutMapping("/{code}")
    public ResponseEntity<TarifPlanResponse> modifierPrix(
            @PathVariable String code,
            @Valid @RequestBody ModifierTarifRequest request
    ) {
        return ResponseEntity.ok(tarifPlanService.modifierPrix(code, request.getPrixMensuel()));
    }
}
