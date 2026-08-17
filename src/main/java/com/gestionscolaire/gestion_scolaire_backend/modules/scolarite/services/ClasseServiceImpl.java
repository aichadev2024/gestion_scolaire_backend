package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

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
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.ClasseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ClasseServiceImpl implements ClasseService {

    @Autowired
    private ClasseRepository classeRepository;

    @Autowired
    private NiveauRepository niveauRepository;

    @Autowired
    private EnseignantRepository enseignantRepository;

    @Override
    public Classe creerClasse(Classe classe, Integer niveauId, Long enseignantPrincipalId) {
        Niveau niveau = niveauRepository.findById(niveauId)
                .orElseThrow(() -> new ResourceNotFoundException("Niveau introuvable"));
        classe.setNiveau(niveau);

        if (classe.getEtablissement() == null) {
            try {
                classe.setEtablissement(com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils.getCurrentUser().getUtilisateur().getEtablissement());
            } catch (Exception ignored) {}
        }

        if (enseignantPrincipalId != null) {
            Enseignant principal = enseignantRepository.findById(enseignantPrincipalId)
                    .orElseThrow(() -> new ResourceNotFoundException("Enseignant introuvable"));
            classe.setEnseignantPrincipal(principal);
        }

        return classeRepository.save(classe);
    }

    @Override
    public Classe modifierClasse(Long id, Classe classeDetails, Integer niveauId, Long enseignantPrincipalId) {
        Classe classe = classeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable"));

        classe.setNom(classeDetails.getNom());
        classe.setCapaciteMax(classeDetails.getCapaciteMax());
        classe.setAnneeScolaire(classeDetails.getAnneeScolaire());

        if (niveauId != null) {
            Niveau niveau = niveauRepository.findById(niveauId)
                    .orElseThrow(() -> new ResourceNotFoundException("Niveau introuvable"));
            classe.setNiveau(niveau);
        }

        if (enseignantPrincipalId != null) {
            Enseignant principal = enseignantRepository.findById(enseignantPrincipalId)
                    .orElseThrow(() -> new ResourceNotFoundException("Enseignant introuvable"));
            classe.setEnseignantPrincipal(principal);
        } else {
            classe.setEnseignantPrincipal(null);
        }

        return classeRepository.save(classe);
    }

    @Override
    public Optional<Classe> trouverParId(Long id) {
        return classeRepository.findById(id);
    }

    @Override
    public List<Classe> listerParNiveau(Integer niveauId) {
        return listerToutes().stream().filter(c -> c.getNiveau() != null && niveauId.equals(c.getNiveau().getId())).toList();
    }

    @Override
    public List<Classe> listerToutes() {
        try {
            com.gestionscolaire.gestion_scolaire_backend.core.security.CustomUserDetails current = com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils.getCurrentUser();
            if (current != null && current.getUtilisateur() != null) {
                if ("SUPER_ADMIN".equalsIgnoreCase(current.getUtilisateur().getRole().getNom())) {
                    return classeRepository.findAll();
                }
                if (current.getUtilisateur().getEtablissement() != null) {
                    Long etabId = current.getUtilisateur().getEtablissement().getId();
                    return classeRepository.findAll().stream()
                            .filter(c -> c.getEtablissement() == null || etabId.equals(c.getEtablissement().getId()))
                            .toList();
                }
            }
        } catch (Exception ignored) {}
        return classeRepository.findAll();
    }
}


