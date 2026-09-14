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
    private final com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard;

    public ClasseMatiereServiceImpl(
            ClasseMatiereRepository classeMatiereRepository,
            ClasseRepository classeRepository,
            MatiereRepository matiereRepository,
            EnseignantRepository enseignantRepository,
            com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard
    ) {
        this.classeMatiereRepository = classeMatiereRepository;
        this.classeRepository = classeRepository;
        this.matiereRepository = matiereRepository;
        this.enseignantRepository = enseignantRepository;
        this.tenantGuard = tenantGuard;
    }

    @Override
    public ClasseMatiere assigner(Long classeId, Long matiereId, Long enseignantId, Double coefficient) {
        Classe classe = tenantGuard.requireSameTenant(classeRepository.findById(classeId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable")));
        tenantGuard.requireSameNiveau(classe, c -> c.getNiveau() != null ? c.getNiveau().getId() : null);
        Matiere matiere = tenantGuard.requireSameTenant(matiereRepository.findById(matiereId)
                .orElseThrow(() -> new ResourceNotFoundException("Matière introuvable")));

        classeMatiereRepository.findByClasseIdAndMatiereId(classeId, matiereId)
                .ifPresent(existing -> {
                    throw new BadRequestException("Cette matière est déjà assignée à la classe");
                });

        ClasseMatiere.ClasseMatiereBuilder builder = ClasseMatiere.builder()
                .classe(classe)
                .matiere(matiere)
                .coefficient(coefficient != null ? coefficient : 1.0);

        if (enseignantId != null) {
            Enseignant enseignant = tenantGuard.requireSameTenant(enseignantRepository.findById(enseignantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Enseignant introuvable")));
            builder.enseignant(enseignant);
        }

        return classeMatiereRepository.save(builder.build());
    }

    @Override
    public ClasseMatiere modifier(Long id, Long enseignantId, Double coefficient) {
        ClasseMatiere cm = tenantGuard.requireSameTenant(classeMatiereRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignation classe-matière introuvable")));
        tenantGuard.requireSameNiveau(cm, x -> x.getClasse() != null && x.getClasse().getNiveau() != null ? x.getClasse().getNiveau().getId() : null);

        if (coefficient != null) {
            cm.setCoefficient(coefficient);
        }
        if (enseignantId != null) {
            Enseignant enseignant = tenantGuard.requireSameTenant(enseignantRepository.findById(enseignantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Enseignant introuvable")));
            cm.setEnseignant(enseignant);
        }

        return classeMatiereRepository.save(cm);
    }

    private Integer niveauDe(ClasseMatiere cm) {
        return cm.getClasse() != null && cm.getClasse().getNiveau() != null ? cm.getClasse().getNiveau().getId() : null;
    }

    @Override
    public Optional<ClasseMatiere> trouverParId(Long id) {
        return classeMatiereRepository.findById(id)
                .filter(tenantGuard::appartientAuTenantCourant)
                .filter(cm -> tenantGuard.correspondAuNiveauCourant(niveauDe(cm)));
    }

    @Override
    public List<ClasseMatiere> listerParClasse(Long classeId) {
        List<ClasseMatiere> classeMatieres = tenantGuard.filterSameNiveau(
                tenantGuard.filterSameTenant(classeMatiereRepository.findByClasseId(classeId)), this::niveauDe);
        return filtrerPourEnseignantConnecte(classeMatieres);
    }

    /**
     * Un compte ENSEIGNANT ne doit voir, dans une classe où il intervient, que la (les) matière(s)
     * qui lui sont réellement assignées — pas celles de ses collègues (sinon la saisie de notes
     * proposerait des matières qu'il n'enseigne pas).
     */
    private List<ClasseMatiere> filtrerPourEnseignantConnecte(List<ClasseMatiere> classeMatieres) {
        com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur utilisateur;
        try {
            utilisateur = com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils.getCurrentUser().getUtilisateur();
        } catch (Exception e) {
            return classeMatieres;
        }
        if (utilisateur.getRole() == null || !"ENSEIGNANT".equalsIgnoreCase(utilisateur.getRole().getNom())) {
            return classeMatieres;
        }
        Enseignant enseignant = enseignantRepository.findByProfilUtilisateurId(utilisateur.getId()).orElse(null);
        if (enseignant == null) {
            return List.of();
        }
        return classeMatieres.stream()
                .filter(cm -> cm.getEnseignant() != null && enseignant.getId().equals(cm.getEnseignant().getId()))
                .toList();
    }

    @Override
    public void supprimer(Long id) {
        ClasseMatiere cm = tenantGuard.requireSameTenant(classeMatiereRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignation classe-matière introuvable")));
        tenantGuard.requireSameNiveau(cm, this::niveauDe);
        classeMatiereRepository.delete(cm);
    }
}


