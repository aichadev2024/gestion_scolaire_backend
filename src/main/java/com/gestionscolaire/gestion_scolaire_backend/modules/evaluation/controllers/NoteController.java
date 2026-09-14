package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.DtoMapper;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto.NoteRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto.NoteResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.Note;
import com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.services.NoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;
    private final DtoMapper dtoMapper;

    public NoteController(NoteService noteService, DtoMapper dtoMapper) {
        this.noteService = noteService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'ENSEIGNANT')")
    public ResponseEntity<NoteResponse> enregistrer(@Valid @RequestBody NoteRequest request) {
        Note note = Note.builder()
                .periode(request.getPeriode())
                .typeEvaluation(request.getTypeEvaluation())
                .valeur(request.getValeur())
                .noteMax(request.getNoteMax())
                .appreciation(request.getAppreciation())
                .build();
        Note saved = noteService.enregistrerNote(note, request.getEleveId(), request.getClasseMatiereId(), SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toNoteResponse(saved));
    }

    @GetMapping("/eleve/{eleveId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'ENSEIGNANT', 'ELEVE', 'PARENT')")
    public ResponseEntity<List<NoteResponse>> listerParEleve(@PathVariable Long eleveId) {
        return ResponseEntity.ok(noteService.listerNotesEleve(eleveId).stream().map(dtoMapper::toNoteResponse).toList());
    }

    @GetMapping("/classe-matiere/{classeMatiereId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'ENSEIGNANT')")
    public ResponseEntity<List<NoteResponse>> listerParClasseMatiere(@PathVariable Long classeMatiereId) {
        return ResponseEntity.ok(noteService.listerNotesParClasseMatiere(classeMatiereId).stream().map(dtoMapper::toNoteResponse).toList());
    }

    @GetMapping("/eleve/{eleveId}/moyenne")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTEUR', 'ENSEIGNANT', 'ELEVE', 'PARENT')")
    public ResponseEntity<Map<String, Double>> calculerMoyenneGenerale(
            @PathVariable Long eleveId,
            @RequestParam String periode
    ) {
        Double moyenne = noteService.calculerMoyenneGeneraleEleve(eleveId, periode);
        return ResponseEntity.ok(Map.of("moyenne", moyenne));
    }
}


