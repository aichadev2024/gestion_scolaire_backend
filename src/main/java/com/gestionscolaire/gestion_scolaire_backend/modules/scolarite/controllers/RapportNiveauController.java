package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.RapportNiveauRequest;
import com.gestionscolaire.gestion_scolaire_backend.core.dto.RapportNiveauResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.RapportNiveauService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rapports-niveau")
public class RapportNiveauController {

    private final RapportNiveauService rapportNiveauService;

    public RapportNiveauController(RapportNiveauService rapportNiveauService) {
        this.rapportNiveauService = rapportNiveauService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTEUR', 'ENSEIGNANT')")
    public ResponseEntity<RapportNiveauResponse> creer(@Valid @RequestBody RapportNiveauRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rapportNiveauService.creer(request));
    }

    /** Élèves de la classe dont la moyenne dans la matière est sous 10 : pré-remplit la liste du rapport. */
    @GetMapping("/en-difficulte")
    @PreAuthorize("hasAnyRole('DIRECTEUR', 'ENSEIGNANT')")
    public ResponseEntity<List<RapportNiveauResponse.EleveEnDifficulte>> enDifficulte(
            @RequestParam Long classeMatiereId, @RequestParam(required = false) String periode) {
        return ResponseEntity.ok(rapportNiveauService.enDifficulte(classeMatiereId, periode));
    }

    @GetMapping("/mes")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<List<RapportNiveauResponse>> mesRapports() {
        return ResponseEntity.ok(rapportNiveauService.mesRapports());
    }

    @GetMapping
    @PreAuthorize("hasRole('DIRECTEUR')")
    public ResponseEntity<List<RapportNiveauResponse>> lister(
            @RequestParam(required = false) Long classeId,
            @RequestParam(defaultValue = "false") boolean nonTraites) {
        return ResponseEntity.ok(rapportNiveauService.lister(classeId, nonTraites));
    }

    @PatchMapping("/{id}/traiter")
    @PreAuthorize("hasRole('DIRECTEUR')")
    public ResponseEntity<RapportNiveauResponse> traiter(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return ResponseEntity.ok(rapportNiveauService.traiter(id, body != null ? body.get("reponse") : null));
    }
}
