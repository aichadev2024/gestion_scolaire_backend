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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard;

    @Override
    public Note enregistrerNote(Note note, Long eleveId, Long classeMatiereId, Long userCreateurId) {
        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable")));

        ClasseMatiere classeMatiere = tenantGuard.requireSameTenant(classeMatiereRepository.findById(classeMatiereId)
                .orElseThrow(() -> new ResourceNotFoundException("ClasseMatiere introuvable")));
        tenantGuard.requireSameNiveau(classeMatiere, this::niveauDe);

        // Validation de note
        if (note.getValeur() < 0 || note.getValeur() > note.getNoteMax()) {
            throw new BadRequestException("La note doit être comprise entre 0 et " + note.getNoteMax());
        }

        note.setEleve(eleve);
        note.setClasseMatiere(classeMatiere);
        note.setEtablissement(eleve.getEtablissement());

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

    private Integer niveauDe(ClasseMatiere cm) {
        return cm.getClasse() != null && cm.getClasse().getNiveau() != null ? cm.getClasse().getNiveau().getId() : null;
    }

    private Integer niveauDe(Note n) {
        return n.getClasseMatiere() != null ? niveauDe(n.getClasseMatiere()) : null;
    }

    @Override
    public List<Note> listerNotesEleve(Long eleveId) {
        return tenantGuard.filterSameNiveau(tenantGuard.filterSameTenant(noteRepository.findByEleveId(eleveId)), this::niveauDe);
    }

    @Override
    public List<Note> listerNotesParClasseMatiere(Long classeMatiereId) {
        return tenantGuard.filterSameNiveau(tenantGuard.filterSameTenant(noteRepository.findByClasseMatiereId(classeMatiereId)), this::niveauDe);
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
        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable")));

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

    @Override
    public Map<String, Double> moyennesParPeriodeMatiere(Long eleveId, Long classeMatiereId) {
        List<Note> notes = noteRepository.findByEleveIdAndClasseMatiereId(eleveId, classeMatiereId);
        Map<String, List<Note>> parPeriode = notes.stream()
                .collect(java.util.stream.Collectors.groupingBy(Note::getPeriode, LinkedHashMap::new, java.util.stream.Collectors.toList()));

        Map<String, Double> moyennes = new LinkedHashMap<>();
        for (Map.Entry<String, List<Note>> entry : parPeriode.entrySet()) {
            double total = entry.getValue().stream().mapToDouble(Note::getValeur).sum();
            moyennes.put(entry.getKey(), total / entry.getValue().size());
        }
        return moyennes;
    }

    @Override
    public Double calculerMoyenneAnnuelleMatiere(Long eleveId, Long classeMatiereId) {
        Map<String, Double> parPeriode = moyennesParPeriodeMatiere(eleveId, classeMatiereId);
        if (parPeriode.isEmpty()) {
            return 0.0;
        }
        // Moyenne des moyennes de période — chaque trimestre/composition compte pour un, peu
        // importe combien de notes y ont été saisies (une évaluation d'écart ne doit pas peser
        // plus que trois compositions juste parce qu'elle a plus de notes derrière elle).
        return parPeriode.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    @Override
    public Double calculerMoyenneGeneraleAnnuelleEleve(Long eleveId) {
        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable")));

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
            Double moyenneAnnuelleMatiere = calculerMoyenneAnnuelleMatiere(eleveId, cm.getId());
            sommeNotesCoeff += (moyenneAnnuelleMatiere * cm.getCoefficient());
            sommeCoeff += cm.getCoefficient();
        }

        return (sommeCoeff == 0) ? 0.0 : (sommeNotesCoeff / sommeCoeff);
    }
}


