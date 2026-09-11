package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Presence;

import java.time.LocalDate;
import java.util.List;

public interface PresenceService {
    Presence enregistrerPresence(Presence presence, Long eleveId, Long classeMatiereId);
    List<Presence> listerPresencesEleve(Long eleveId);
    List<Presence> listerPresencesParClasseMatiereEtDate(Long classeMatiereId, LocalDate date);

    /**
     * Un parent justifie l'absence/le retard de son enfant. Vérifie que
     * l'appelant est bien un parent de l'élève concerné, puis notifie la
     * direction de l'établissement.
     */
    Presence justifierAbsence(Long presenceId, String notesJustification);
}


