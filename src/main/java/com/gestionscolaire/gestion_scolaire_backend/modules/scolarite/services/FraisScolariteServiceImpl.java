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
    private final com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard;

    public FraisScolariteServiceImpl(FraisScolariteRepository fraisScolariteRepository, ClasseRepository classeRepository,
                                     com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard) {
        this.fraisScolariteRepository = fraisScolariteRepository;
        this.classeRepository = classeRepository;
        this.tenantGuard = tenantGuard;
    }

    @Override
    public FraisScolarite creerFrais(FraisScolarite frais, Long classeId) {
        Classe classe = tenantGuard.requireSameTenant(classeRepository.findById(classeId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable")));
        tenantGuard.requireSameNiveau(classe, c -> c.getNiveau() != null ? c.getNiveau().getId() : null);
        frais.setClasse(classe);
        frais.setEtablissement(classe.getEtablissement());
        return fraisScolariteRepository.save(frais);
    }

    @Override
    public FraisScolarite modifierFrais(Long id, FraisScolarite fraisDetails) {
        FraisScolarite frais = tenantGuard.requireSameTenant(fraisScolariteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Frais de scolarité introuvables")));
        tenantGuard.requireSameNiveau(frais, this::niveauDe);
        frais.setTitre(fraisDetails.getTitre());
        frais.setMontant(fraisDetails.getMontant());
        frais.setDateEcheance(fraisDetails.getDateEcheance());
        return fraisScolariteRepository.save(frais);
    }

    private Integer niveauDe(FraisScolarite f) {
        return f.getClasse() != null && f.getClasse().getNiveau() != null ? f.getClasse().getNiveau().getId() : null;
    }

    @Override
    public Optional<FraisScolarite> trouverParId(Long id) {
        return fraisScolariteRepository.findById(id)
                .filter(tenantGuard::appartientAuTenantCourant)
                .filter(f -> tenantGuard.correspondAuNiveauCourant(niveauDe(f)));
    }

    @Override
    public List<FraisScolarite> listerParClasse(Long classeId) {
        return tenantGuard.filterSameNiveau(tenantGuard.filterSameTenant(fraisScolariteRepository.findByClasseId(classeId)), this::niveauDe);
    }

    @Override
    public List<FraisScolarite> listerTous() {
        List<FraisScolarite> frais;
        if (tenantGuard.crossTenant()) {
            frais = fraisScolariteRepository.findAll();
        } else {
            frais = fraisScolariteRepository.findByEtablissementId(tenantGuard.requireEtablissementId());
        }
        return tenantGuard.filterSameNiveau(frais, this::niveauDe);
    }

    @Override
    public void supprimerFrais(Long id) {
        FraisScolarite frais = tenantGuard.requireSameTenant(fraisScolariteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Frais de scolarité introuvables")));
        tenantGuard.requireSameNiveau(frais, this::niveauDe);
        fraisScolariteRepository.delete(frais);
    }
}


