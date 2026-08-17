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

    public PresenceEnseignantServiceImpl(
            PresenceEnseignantRepository presenceEnseignantRepository,
            EnseignantRepository enseignantRepository
    ) {
        this.presenceEnseignantRepository = presenceEnseignantRepository;
        this.enseignantRepository = enseignantRepository;
    }

    @Override
    public PresenceEnseignant enregistrerPresence(PresenceEnseignant presence, Long enseignantId) {
        Enseignant enseignant = enseignantRepository.findById(enseignantId)
                .orElseThrow(() -> new ResourceNotFoundException("Enseignant introuvable"));

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
        return presenceEnseignantRepository.save(presence);
    }

    @Override
    public List<PresenceEnseignant> listerParDate(LocalDate date) {
        return presenceEnseignantRepository.findByDate(date);
    }

    @Override
    public List<PresenceEnseignant> listerParEnseignant(Long enseignantId) {
        return presenceEnseignantRepository.findByEnseignantId(enseignantId);
    }
}
