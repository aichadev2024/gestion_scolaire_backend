package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.PresenceEnseignant;

import java.time.LocalDate;
import java.util.List;

public interface PresenceEnseignantService {
    PresenceEnseignant enregistrerPresence(PresenceEnseignant presence, Long enseignantId);
    List<PresenceEnseignant> listerParDate(LocalDate date);
    List<PresenceEnseignant> listerParEnseignant(Long enseignantId);
}
