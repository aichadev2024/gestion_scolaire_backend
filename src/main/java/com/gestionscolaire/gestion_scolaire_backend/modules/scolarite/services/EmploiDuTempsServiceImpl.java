package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.ClasseMatiere;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.EmploiDuTemps;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseMatiereRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EmploiDuTempsRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.EmploiDuTempsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EmploiDuTempsServiceImpl implements EmploiDuTempsService {

    private final EmploiDuTempsRepository emploiDuTempsRepository;
    private final ClasseMatiereRepository classeMatiereRepository;
    private final com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseRepository classeRepository;

    public EmploiDuTempsServiceImpl(
            EmploiDuTempsRepository emploiDuTempsRepository,
            ClasseMatiereRepository classeMatiereRepository,
            com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseRepository classeRepository
    ) {
        this.emploiDuTempsRepository = emploiDuTempsRepository;
        this.classeMatiereRepository = classeMatiereRepository;
        this.classeRepository = classeRepository;
    }

    @Override
    public EmploiDuTemps creerCreneau(EmploiDuTemps creneau, Long classeMatiereId) {
        if (classeMatiereId != null) {
            ClasseMatiere classeMatiere = classeMatiereRepository.findById(classeMatiereId)
                    .orElseThrow(() -> new ResourceNotFoundException("Classe-matière introuvable"));
            creneau.setClasseMatiere(classeMatiere);
            creneau.setClasse(classeMatiere.getClasse());
        } else if (creneau.getClasse() != null && creneau.getClasse().getId() != null) {
            com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe c = classeRepository.findById(creneau.getClasse().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable"));
            creneau.setClasse(c);
        }
        validerCreneau(creneau);
        return emploiDuTempsRepository.save(creneau);
    }

    @Override
    public EmploiDuTemps modifierCreneau(Long id, EmploiDuTemps creneauDetails, Long classeMatiereId) {
        EmploiDuTemps creneau = emploiDuTempsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Créneau introuvable"));
        validerCreneau(creneauDetails);

        creneau.setJourSemaine(creneauDetails.getJourSemaine());
        creneau.setHeureDebut(creneauDetails.getHeureDebut());
        creneau.setHeureFin(creneauDetails.getHeureFin());
        creneau.setSalle(creneauDetails.getSalle());
        creneau.setTypeCreneau(creneauDetails.getTypeCreneau());
        creneau.setLibellePause(creneauDetails.getLibellePause());

        if (classeMatiereId != null) {
            ClasseMatiere classeMatiere = classeMatiereRepository.findById(classeMatiereId)
                    .orElseThrow(() -> new ResourceNotFoundException("Classe-matière introuvable"));
            creneau.setClasseMatiere(classeMatiere);
            creneau.setClasse(classeMatiere.getClasse());
        } else if (creneauDetails.getClasse() != null && creneauDetails.getClasse().getId() != null) {
            com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe c = classeRepository.findById(creneauDetails.getClasse().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable"));
            creneau.setClasse(c);
        }

        return emploiDuTempsRepository.save(creneau);
    }

    @Override
    public Optional<EmploiDuTemps> trouverParId(Long id) {
        return emploiDuTempsRepository.findById(id);
    }

    @Override
    public List<EmploiDuTemps> listerParClasse(Long classeId) {
        return emploiDuTempsRepository.findByClasseIdOrClasseMatiereClasseId(classeId, classeId);
    }

    @Override
    public List<EmploiDuTemps> listerParEnseignant(Long enseignantId) {
        return emploiDuTempsRepository.findByClasseMatiereEnseignantId(enseignantId);
    }

    @Override
    public void supprimerCreneau(Long id) {
        if (!emploiDuTempsRepository.existsById(id)) {
            throw new ResourceNotFoundException("Créneau introuvable");
        }
        emploiDuTempsRepository.deleteById(id);
    }

    private void validerCreneau(EmploiDuTemps creneau) {
        if (creneau.getJourSemaine() == null || creneau.getJourSemaine() < 1 || creneau.getJourSemaine() > 7) {
            throw new BadRequestException("Le jour de la semaine doit être entre 1 et 7");
        }
        if (creneau.getHeureDebut() == null || creneau.getHeureFin() == null) {
            throw new BadRequestException("Les heures de début et de fin sont obligatoires");
        }
        if (creneau.getHeureFin().isBefore(creneau.getHeureDebut()) || creneau.getHeureFin().equals(creneau.getHeureDebut())) {
            throw new BadRequestException("L'heure de fin doit être après l'heure de début");
        }
        if (creneau.getClasseMatiere() == null && creneau.getClasse() == null) {
            throw new BadRequestException("Une classe ou une classe-matière doit être associée au créneau");
        }
    }
}


