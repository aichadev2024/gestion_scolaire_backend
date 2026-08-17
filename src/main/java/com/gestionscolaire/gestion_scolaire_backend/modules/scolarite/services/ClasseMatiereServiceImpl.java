package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.ClasseMatiere;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Enseignant;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Matiere;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseMatiereRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EnseignantRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.MatiereRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.ClasseMatiereService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ClasseMatiereServiceImpl implements ClasseMatiereService {

    private final ClasseMatiereRepository classeMatiereRepository;
    private final ClasseRepository classeRepository;
    private final MatiereRepository matiereRepository;
    private final EnseignantRepository enseignantRepository;

    public ClasseMatiereServiceImpl(
            ClasseMatiereRepository classeMatiereRepository,
            ClasseRepository classeRepository,
            MatiereRepository matiereRepository,
            EnseignantRepository enseignantRepository
    ) {
        this.classeMatiereRepository = classeMatiereRepository;
        this.classeRepository = classeRepository;
        this.matiereRepository = matiereRepository;
        this.enseignantRepository = enseignantRepository;
    }

    @Override
    public ClasseMatiere assigner(Long classeId, Long matiereId, Long enseignantId, Double coefficient) {
        Classe classe = classeRepository.findById(classeId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable"));
        Matiere matiere = matiereRepository.findById(matiereId)
                .orElseThrow(() -> new ResourceNotFoundException("Matière introuvable"));

        classeMatiereRepository.findByClasseIdAndMatiereId(classeId, matiereId)
                .ifPresent(existing -> {
                    throw new BadRequestException("Cette matière est déjà assignée à la classe");
                });

        ClasseMatiere.ClasseMatiereBuilder builder = ClasseMatiere.builder()
                .classe(classe)
                .matiere(matiere)
                .coefficient(coefficient != null ? coefficient : 1.0);

        if (enseignantId != null) {
            Enseignant enseignant = enseignantRepository.findById(enseignantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Enseignant introuvable"));
            builder.enseignant(enseignant);
        }

        return classeMatiereRepository.save(builder.build());
    }

    @Override
    public ClasseMatiere modifier(Long id, Long enseignantId, Double coefficient) {
        ClasseMatiere cm = classeMatiereRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignation classe-matière introuvable"));

        if (coefficient != null) {
            cm.setCoefficient(coefficient);
        }
        if (enseignantId != null) {
            Enseignant enseignant = enseignantRepository.findById(enseignantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Enseignant introuvable"));
            cm.setEnseignant(enseignant);
        }

        return classeMatiereRepository.save(cm);
    }

    @Override
    public Optional<ClasseMatiere> trouverParId(Long id) {
        return classeMatiereRepository.findById(id);
    }

    @Override
    public List<ClasseMatiere> listerParClasse(Long classeId) {
        try {
            com.gestionscolaire.gestion_scolaire_backend.core.security.CustomUserDetails current = com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils.getCurrentUser();
            if (current != null && current.getUtilisateur() != null) {
                if ("SUPER_ADMIN".equalsIgnoreCase(current.getUtilisateur().getRole().getNom())) {
                    return classeMatiereRepository.findByClasseId(classeId);
                }
                if (current.getUtilisateur().getEtablissement() != null) {
                    Long etabId = current.getUtilisateur().getEtablissement().getId();
                    return classeMatiereRepository.findByClasseId(classeId).stream()
                            .filter(cm -> cm.getClasse() == null || cm.getClasse().getEtablissement() == null || etabId.equals(cm.getClasse().getEtablissement().getId()))
                            .toList();
                }
            }
        } catch (Exception ignored) {}
        return classeMatiereRepository.findByClasseId(classeId);
    }

    @Override
    public void supprimer(Long id) {
        if (!classeMatiereRepository.existsById(id)) {
            throw new ResourceNotFoundException("Assignation classe-matière introuvable");
        }
        classeMatiereRepository.deleteById(id);
    }
}


