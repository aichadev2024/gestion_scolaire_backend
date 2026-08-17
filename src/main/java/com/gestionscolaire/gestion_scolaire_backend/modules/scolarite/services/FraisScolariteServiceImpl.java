package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.FraisScolarite;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.FraisScolariteRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.FraisScolariteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FraisScolariteServiceImpl implements FraisScolariteService {

    private final FraisScolariteRepository fraisScolariteRepository;
    private final ClasseRepository classeRepository;

    public FraisScolariteServiceImpl(FraisScolariteRepository fraisScolariteRepository, ClasseRepository classeRepository) {
        this.fraisScolariteRepository = fraisScolariteRepository;
        this.classeRepository = classeRepository;
    }

    @Override
    public FraisScolarite creerFrais(FraisScolarite frais, Long classeId) {
        Classe classe = classeRepository.findById(classeId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable"));
        frais.setClasse(classe);
        return fraisScolariteRepository.save(frais);
    }

    @Override
    public FraisScolarite modifierFrais(Long id, FraisScolarite fraisDetails) {
        FraisScolarite frais = fraisScolariteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Frais de scolarité introuvables"));
        frais.setTitre(fraisDetails.getTitre());
        frais.setMontant(fraisDetails.getMontant());
        frais.setDateEcheance(fraisDetails.getDateEcheance());
        return fraisScolariteRepository.save(frais);
    }

    @Override
    public Optional<FraisScolarite> trouverParId(Long id) {
        return fraisScolariteRepository.findById(id);
    }

    @Override
    public List<FraisScolarite> listerParClasse(Long classeId) {
        try {
            com.gestionscolaire.gestion_scolaire_backend.core.security.CustomUserDetails current = com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils.getCurrentUser();
            if (current != null && current.getUtilisateur() != null) {
                if ("SUPER_ADMIN".equalsIgnoreCase(current.getUtilisateur().getRole().getNom())) {
                    return fraisScolariteRepository.findByClasseId(classeId);
                }
                if (current.getUtilisateur().getEtablissement() != null) {
                    Long etabId = current.getUtilisateur().getEtablissement().getId();
                    return fraisScolariteRepository.findByClasseId(classeId).stream()
                            .filter(f -> f.getClasse() == null || f.getClasse().getEtablissement() == null || etabId.equals(f.getClasse().getEtablissement().getId()))
                            .toList();
                }
            }
        } catch (Exception ignored) {}
        return fraisScolariteRepository.findByClasseId(classeId);
    }

    @Override
    public void supprimerFrais(Long id) {
        if (!fraisScolariteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Frais de scolarité introuvables");
        }
        fraisScolariteRepository.deleteById(id);
    }
}


