package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Enseignant;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.PresenceEnseignant;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EnseignantRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.PresenceEnseignantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PresenceEnseignantServiceImpl implements PresenceEnseignantService {

    private final PresenceEnseignantRepository presenceEnseignantRepository;
    private final EnseignantRepository enseignantRepository;
    private final com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseMatiereRepository classeMatiereRepository;
    private final com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard;

    public PresenceEnseignantServiceImpl(
            PresenceEnseignantRepository presenceEnseignantRepository,
            EnseignantRepository enseignantRepository,
            com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseMatiereRepository classeMatiereRepository,
            com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard
    ) {
        this.presenceEnseignantRepository = presenceEnseignantRepository;
        this.enseignantRepository = enseignantRepository;
        this.classeMatiereRepository = classeMatiereRepository;
        this.tenantGuard = tenantGuard;
    }

    @Override
    public PresenceEnseignant enregistrerPresence(PresenceEnseignant presence, Long enseignantId) {
        Enseignant enseignant = tenantGuard.requireSameTenant(enseignantRepository.findById(enseignantId)
                .orElseThrow(() -> new ResourceNotFoundException("Enseignant introuvable")));

        Optional<PresenceEnseignant> dejaExiste = presenceEnseignantRepository.findByEnseignantIdAndDate(enseignantId, presence.getDate());
        if (dejaExiste.isPresent()) {
            PresenceEnseignant p = dejaExiste.get();
            p.setStatut(presence.getStatut());
            p.setHeureArrivee(presence.getHeureArrivee());
            p.setHeureDepart(presence.getHeureDepart());
            p.setRemarques(presence.getRemarques());
            return presenceEnseignantRepository.save(p);
        }

        presence.setEnseignant(enseignant);
        presence.setEtablissement(enseignant.getEtablissement());
        return presenceEnseignantRepository.save(presence);
    }

    @Override
    public List<PresenceEnseignant> listerParDate(LocalDate date) {
        if (tenantGuard.crossTenant()) {
            return presenceEnseignantRepository.findByDate(date);
        }
        return presenceEnseignantRepository.findByEtablissementIdAndDate(tenantGuard.requireEtablissementId(), date);
    }

    @Override
    public List<PresenceEnseignant> listerParEnseignant(Long enseignantId) {
        return tenantGuard.filterSameTenant(presenceEnseignantRepository.findByEnseignantId(enseignantId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.gestionscolaire.gestion_scolaire_backend.core.dto.EmargementEnseignantResponse> listerFiche(LocalDate debut, LocalDate fin) {
        if (fin.isBefore(debut)) {
            throw new com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException("La date de fin doit suivre la date de début.");
        }
        if (debut.plusDays(400).isBefore(fin)) {
            throw new com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException("La période ne peut pas dépasser 13 mois.");
        }
        java.util.Map<Long, List<String>> niveauxParEnseignant = new java.util.HashMap<>();
        return presenceEnseignantRepository
                .findByEtablissementIdAndDateBetweenOrderByDateAsc(tenantGuard.requireEtablissementId(), debut, fin)
                .stream()
                .map(p -> {
                    Enseignant e = p.getEnseignant();
                    List<String> niveaux = niveauxParEnseignant.computeIfAbsent(e.getId(), id ->
                            classeMatiereRepository.findByEnseignantId(id).stream()
                                    .map(cm -> cm.getClasse() != null && cm.getClasse().getNiveau() != null ? cm.getClasse().getNiveau().getNom() : null)
                                    .filter(java.util.Objects::nonNull)
                                    .distinct()
                                    .sorted()
                                    .toList());
                    return new com.gestionscolaire.gestion_scolaire_backend.core.dto.EmargementEnseignantResponse(
                            p.getId(), e.getId(), e.getMatricule(),
                            e.getProfil() != null ? e.getProfil().getNom() : null,
                            e.getProfil() != null ? e.getProfil().getPrenom() : null,
                            p.getDate(), p.getStatut(), p.getHeureArrivee(), p.getHeureDepart(), p.getRemarques(), niveaux);
                })
                .toList();
    }
}
