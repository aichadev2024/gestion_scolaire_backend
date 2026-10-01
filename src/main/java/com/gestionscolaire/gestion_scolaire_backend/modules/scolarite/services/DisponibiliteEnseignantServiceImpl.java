package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.DisponibiliteEnseignant;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Enseignant;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.DisponibiliteEnseignantRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EnseignantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class DisponibiliteEnseignantServiceImpl implements DisponibiliteEnseignantService {

    private static final Set<String> TYPES_VALIDES = Set.of("DISPONIBLE", "INDISPONIBLE");

    private final DisponibiliteEnseignantRepository disponibiliteRepository;
    private final EnseignantRepository enseignantRepository;
    private final TenantGuard tenantGuard;

    public DisponibiliteEnseignantServiceImpl(
            DisponibiliteEnseignantRepository disponibiliteRepository,
            EnseignantRepository enseignantRepository,
            TenantGuard tenantGuard
    ) {
        this.disponibiliteRepository = disponibiliteRepository;
        this.enseignantRepository = enseignantRepository;
        this.tenantGuard = tenantGuard;
    }

    @Override
    public DisponibiliteEnseignant creerPourMoi(DisponibiliteEnseignant dispo) {
        Enseignant enseignant = resolveEnseignantCourant();
        validerCreneau(dispo);
        dispo.setEnseignant(enseignant);
        dispo.setEtablissement(enseignant.getEtablissement());
        return disponibiliteRepository.save(dispo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisponibiliteEnseignant> listerPourMoi() {
        Enseignant enseignant = resolveEnseignantCourant();
        return disponibiliteRepository.findByEnseignantIdOrderByJourSemaineAscHeureDebutAsc(enseignant.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisponibiliteEnseignant> listerParEnseignant(Long enseignantId) {
        return tenantGuard.filterSameTenant(
                disponibiliteRepository.findByEnseignantIdOrderByJourSemaineAscHeureDebutAsc(enseignantId));
    }

    @Override
    public void supprimer(Long id) {
        DisponibiliteEnseignant dispo = tenantGuard.requireSameTenant(disponibiliteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Disponibilité introuvable")));
        Enseignant enseignant = resolveEnseignantCourant();
        if (dispo.getEnseignant() == null || !dispo.getEnseignant().getId().equals(enseignant.getId())) {
            throw new BadRequestException("Vous ne pouvez supprimer que vos propres disponibilités");
        }
        disponibiliteRepository.delete(dispo);
    }

    private Enseignant resolveEnseignantCourant() {
        Long utilisateurId = SecurityUtils.getCurrentUserId();
        return enseignantRepository.findByProfilUtilisateurId(utilisateurId)
                .orElseThrow(() -> new BadRequestException("Aucune fiche enseignant associée à ce compte"));
    }

    private void validerCreneau(DisponibiliteEnseignant dispo) {
        if (dispo.getJourSemaine() == null || dispo.getJourSemaine() < 1 || dispo.getJourSemaine() > 6) {
            throw new BadRequestException("Le jour de la semaine doit être entre 1 (lundi) et 6 (samedi)");
        }
        if (dispo.getHeureDebut() == null || dispo.getHeureFin() == null) {
            throw new BadRequestException("Les heures de début et de fin sont obligatoires");
        }
        if (!dispo.getHeureFin().isAfter(dispo.getHeureDebut())) {
            throw new BadRequestException("L'heure de fin doit être après l'heure de début");
        }
        if (dispo.getType() == null || dispo.getType().isBlank()) {
            dispo.setType("DISPONIBLE");
        } else if (!TYPES_VALIDES.contains(dispo.getType().toUpperCase())) {
            throw new BadRequestException("Type de disponibilité invalide (DISPONIBLE ou INDISPONIBLE)");
        } else {
            dispo.setType(dispo.getType().toUpperCase());
        }
    }
}
