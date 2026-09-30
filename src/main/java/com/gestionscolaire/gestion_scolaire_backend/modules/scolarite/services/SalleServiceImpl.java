package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Salle;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.SalleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SalleServiceImpl implements SalleService {

    private final SalleRepository salleRepository;
    private final TenantGuard tenantGuard;

    public SalleServiceImpl(SalleRepository salleRepository, TenantGuard tenantGuard) {
        this.salleRepository = salleRepository;
        this.tenantGuard = tenantGuard;
    }

    @Override
    public Salle creerSalle(Salle salle) {
        Long etablissementId = tenantGuard.requireEtablissementId();
        String nom = salle.getNom().trim();
        if (salleRepository.existsByEtablissementIdAndNomIgnoreCase(etablissementId, nom)) {
            throw new BadRequestException("Une salle portant ce nom existe déjà");
        }
        salle.setNom(nom);
        return salleRepository.save(salle);
    }

    @Override
    public Salle modifierSalle(Long id, Salle salleDetails) {
        Salle salle = tenantGuard.requireSameTenant(salleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable")));
        String nom = salleDetails.getNom().trim();
        if (!salle.getNom().equalsIgnoreCase(nom)
                && salleRepository.existsByEtablissementIdAndNomIgnoreCase(tenantGuard.requireEtablissementId(), nom)) {
            throw new BadRequestException("Une salle portant ce nom existe déjà");
        }
        salle.setNom(nom);
        salle.setCapacite(salleDetails.getCapacite());
        return salleRepository.save(salle);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Salle> trouverParId(Long id) {
        return salleRepository.findById(id).filter(tenantGuard::appartientAuTenantCourant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Salle> listerToutes() {
        return salleRepository.findByEtablissementIdOrderByNomAsc(tenantGuard.requireEtablissementId());
    }

    @Override
    public void supprimerSalle(Long id) {
        Salle salle = tenantGuard.requireSameTenant(salleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable")));
        salleRepository.delete(salle);
    }
}
