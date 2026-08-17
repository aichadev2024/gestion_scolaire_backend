package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.services.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NoteServiceImpl implements NoteService {

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private EleveRepository eleveRepository;

    @Autowired
    private BulletinRepository bulletinRepository;

    @Autowired
    private ClasseMatiereRepository classeMatiereRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Override
    public Note enregistrerNote(Note note, Long eleveId, Long classeMatiereId, Long userCreateurId) {
        Eleve eleve = eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable"));
        
        ClasseMatiere classeMatiere = classeMatiereRepository.findById(classeMatiereId)
                .orElseThrow(() -> new ResourceNotFoundException("ClasseMatiere introuvable"));

        // Validation de note
        if (note.getValeur() < 0 || note.getValeur() > note.getNoteMax()) {
            throw new BadRequestException("La note doit être comprise entre 0 et " + note.getNoteMax());
        }

        note.setEleve(eleve);
        note.setClasseMatiere(classeMatiere);

        // Verification du verrouillage du bulletin
        bulletinRepository.findByEleveIdAndPeriodeAndAnneeScolaire(
                eleveId, note.getPeriode(), classeMatiere.getClasse().getAnneeScolaire())
            .ifPresent(bulletin -> {
                if (bulletin.getEstVerrouille()) {
                    throw new BadRequestException("Impossible d'ajouter/modifier une note : le bulletin pour cette période est verrouillé.");
                }
            });

        if (userCreateurId != null) {
            Utilisateur createur = utilisateurRepository.findById(userCreateurId).orElse(null);
            note.setCreePar(createur);
        }

        return noteRepository.save(note);
    }

    @Override
    public List<Note> listerNotesEleve(Long eleveId) {
        return noteRepository.findByEleveId(eleveId);
    }

    @Override
    public List<Note> listerNotesParClasseMatiere(Long classeMatiereId) {
        return noteRepository.findByClasseMatiereId(classeMatiereId);
    }

    @Override
    public Double calculerMoyenneEleveParMatiere(Long eleveId, Long classeMatiereId, String periode) {
        List<Note> notes = noteRepository.findByEleveIdAndClasseMatiereId(eleveId, classeMatiereId);
        
        // Filtrage des notes par période (trimestre / semestre)
        List<Note> notesPeriode = notes.stream()
                .filter(n -> n.getPeriode().equalsIgnoreCase(periode))
                .toList();

        if (notesPeriode.isEmpty()) {
            return 0.0;
        }

        double total = notesPeriode.stream().mapToDouble(Note::getValeur).sum();
        return total / notesPeriode.size();
    }

    @Override
    public Double calculerMoyenneGeneraleEleve(Long eleveId, String periode) {
        Eleve eleve = eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable"));

        if (eleve.getClasse() == null) {
            return 0.0;
        }

        List<ClasseMatiere> matieres = classeMatiereRepository.findByClasseId(eleve.getClasse().getId());
        if (matieres.isEmpty()) {
            return 0.0;
        }

        double sommeNotesCoeff = 0.0;
        double sommeCoeff = 0.0;

        for (ClasseMatiere cm : matieres) {
            Double moyenneMatiere = calculerMoyenneEleveParMatiere(eleveId, cm.getId(), periode);
            sommeNotesCoeff += (moyenneMatiere * cm.getCoefficient());
            sommeCoeff += cm.getCoefficient();
        }

        return (sommeCoeff == 0) ? 0.0 : (sommeNotesCoeff / sommeCoeff);
    }
}


