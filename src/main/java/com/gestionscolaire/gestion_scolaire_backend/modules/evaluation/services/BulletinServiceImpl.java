package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto.BulletinLigneResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto.BulletinResponse;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.Bulletin;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.ClasseMatiere;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.Note;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.repositories.BulletinRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseMatiereRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EleveRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.repositories.NoteRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.services.BulletinService;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.services.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BulletinServiceImpl implements BulletinService {

    @Autowired
    private BulletinRepository bulletinRepository;

    @Autowired
    private EleveRepository eleveRepository;

    @Autowired
    private ClasseMatiereRepository classeMatiereRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private NoteService noteService;

    @Override
    public BulletinResponse genererBulletin(Long eleveId, String periode, String anneeScolaire) {
        Eleve eleve = eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable"));

        if (eleve.getClasse() == null) {
            throw new BadRequestException("L'élève n'est affecté à aucune classe.");
        }

        Bulletin bulletin = bulletinRepository.findByEleveIdAndPeriodeAndAnneeScolaire(eleveId, periode, anneeScolaire)
                .orElse(Bulletin.builder()
                        .eleve(eleve)
                        .classe(eleve.getClasse())
                        .periode(periode)
                        .anneeScolaire(anneeScolaire)
                        .estVerrouille(false)
                        .build());

        if (bulletin.getEstVerrouille()) {
            throw new BadRequestException("Le bulletin est verrouillé, impossible de le regénérer.");
        }

        Double moyenneGenerale = noteService.calculerMoyenneGeneraleEleve(eleveId, periode);
        bulletin.setMoyenneGenerale(moyenneGenerale);
        
        // Simple appreciation logic for MVP
        if (moyenneGenerale >= 16) bulletin.setAppreciationGenerale("Très bien");
        else if (moyenneGenerale >= 14) bulletin.setAppreciationGenerale("Bien");
        else if (moyenneGenerale >= 12) bulletin.setAppreciationGenerale("Assez bien");
        else if (moyenneGenerale >= 10) bulletin.setAppreciationGenerale("Passable");
        else bulletin.setAppreciationGenerale("Insuffisant");

        bulletin = bulletinRepository.save(bulletin);

        return getBulletinDetails(eleveId, periode, anneeScolaire);
    }

    @Override
    public BulletinResponse getBulletinDetails(Long eleveId, String periode, String anneeScolaire) {
        Eleve eleve = eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable"));

        Bulletin bulletin = bulletinRepository.findByEleveIdAndPeriodeAndAnneeScolaire(eleveId, periode, anneeScolaire)
                .orElse(null);

        List<ClasseMatiere> matieres = new ArrayList<>();
        if (eleve.getClasse() != null) {
            matieres = classeMatiereRepository.findByClasseId(eleve.getClasse().getId());
        }

        List<BulletinLigneResponse> lignes = new ArrayList<>();

        for (ClasseMatiere cm : matieres) {
            List<Note> notesMatiere = noteRepository.findByEleveIdAndClasseMatiereId(eleveId, cm.getId())
                    .stream()
                    .filter(n -> n.getPeriode().equalsIgnoreCase(periode))
                    .collect(Collectors.toList());

            Double moyenneMatiere = noteService.calculerMoyenneEleveParMatiere(eleveId, cm.getId(), periode);

            List<BulletinLigneResponse.NoteDetail> noteDetails = notesMatiere.stream().map(n -> 
                BulletinLigneResponse.NoteDetail.builder()
                    .id(n.getId())
                    .valeur(n.getValeur())
                    .noteMax(n.getNoteMax())
                    .typeEvaluation(n.getTypeEvaluation())
                    .appreciation(n.getAppreciation())
                    .build()
            ).collect(Collectors.toList());

            lignes.add(BulletinLigneResponse.builder()
                    .classeMatiereId(cm.getId())
                    .matiereNom(cm.getMatiere().getNom())
                    .coefficient(cm.getCoefficient())
                    .moyenneEleve(moyenneMatiere)
                    .notes(noteDetails)
                    .build());
        }

        return BulletinResponse.builder()
                .id(bulletin != null ? bulletin.getId() : null)
                .eleveId(eleve.getId())
                .eleveNom(eleve.getProfil().getNom())
                .elevePrenom(eleve.getProfil().getPrenom())
                .eleveMatricule(eleve.getMatricule())
                .classeId(eleve.getClasse() != null ? eleve.getClasse().getId() : null)
                .classeNom(eleve.getClasse() != null ? eleve.getClasse().getNom() : null)
                .periode(periode)
                .anneeScolaire(anneeScolaire)
                .moyenneGenerale(bulletin != null ? bulletin.getMoyenneGenerale() : null)
                .appreciationGenerale(bulletin != null ? bulletin.getAppreciationGenerale() : null)
                .estVerrouille(bulletin != null ? bulletin.getEstVerrouille() : false)
                .dateCreation(bulletin != null ? bulletin.getDateCreation() : null)
                .dateModification(bulletin != null ? bulletin.getDateModification() : null)
                .lignes(lignes)
                .build();
    }

    @Override
    public BulletinResponse verrouillerBulletin(Long bulletinId) {
        Bulletin bulletin = bulletinRepository.findById(bulletinId)
                .orElseThrow(() -> new ResourceNotFoundException("Bulletin introuvable"));
        
        bulletin.setEstVerrouille(true);
        bulletin = bulletinRepository.save(bulletin);

        return getBulletinDetails(bulletin.getEleve().getId(), bulletin.getPeriode(), bulletin.getAnneeScolaire());
    }
}


