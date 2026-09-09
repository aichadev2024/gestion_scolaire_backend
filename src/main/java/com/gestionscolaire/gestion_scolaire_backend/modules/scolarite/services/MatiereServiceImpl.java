package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Matiere;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.MatiereRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.MatiereService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MatiereServiceImpl implements MatiereService {

    private final MatiereRepository matiereRepository;
    private final com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard;

    public MatiereServiceImpl(MatiereRepository matiereRepository,
                              com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard) {
        this.matiereRepository = matiereRepository;
        this.tenantGuard = tenantGuard;
    }

    private String genererCodeAutomatique(String nom) {
        if (nom == null || nom.isBlank()) return "MAT-" + (System.currentTimeMillis() % 10000);
        String clean = nom.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        String prefix = clean.length() >= 4 ? clean.substring(0, 4) : clean;
        if (prefix.isBlank()) prefix = "MAT";

        String codeGenere = prefix;
        int counter = 1;
        while (matiereRepository.findByCode(codeGenere).isPresent()) {
            codeGenere = prefix + "-" + counter++;
        }
        return codeGenere;
    }

    @Override
    public Matiere creerMatiere(Matiere matiere) {
        if (matiere.getCode() == null || matiere.getCode().isBlank()) {
            matiere.setCode(genererCodeAutomatique(matiere.getNom()));
        } else {
            if (matiereRepository.findByCode(matiere.getCode()).isPresent()) {
                throw new BadRequestException("Une matière avec ce code existe déjà");
            }
        }
        if (matiere.getEtablissement() == null) {
            try {
                matiere.setEtablissement(com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils.getCurrentUser().getUtilisateur().getEtablissement());
            } catch (Exception ignored) {}
        }
        return matiereRepository.save(matiere);
    }

    @Override
    public Matiere modifierMatiere(Long id, Matiere matiereDetails) {
        Matiere matiere = tenantGuard.requireSameTenant(matiereRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matière introuvable")));

        if (matiereDetails.getCode() == null || matiereDetails.getCode().isBlank()) {
            if (matiere.getCode() == null || matiere.getCode().isBlank()) {
                matiere.setCode(genererCodeAutomatique(matiereDetails.getNom()));
            }
        } else {
            matiereRepository.findByCode(matiereDetails.getCode())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new BadRequestException("Une matière avec ce code existe déjà");
                    });
            matiere.setCode(matiereDetails.getCode());
        }

        matiere.setNom(matiereDetails.getNom());
        return matiereRepository.save(matiere);
    }

    @Override
    public Optional<Matiere> trouverParId(Long id) {
        return matiereRepository.findById(id).filter(tenantGuard::appartientAuTenantCourant);
    }

    @Override
    public Optional<Matiere> trouverParCode(String code) {
        return matiereRepository.findByCode(code);
    }

    @Override
    public List<Matiere> listerToutes() {
        if (tenantGuard.crossTenant()) {
            return matiereRepository.findAll();
        }
        return matiereRepository.findByEtablissementId(tenantGuard.requireEtablissementId());
    }

    @Override
    public void supprimerMatiere(Long id) {
        Matiere matiere = tenantGuard.requireSameTenant(matiereRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matière introuvable")));
        matiereRepository.delete(matiere);
    }
}


