package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.IncidentDisciplineResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.IncidentDiscipline;

import java.time.LocalDate;
import java.util.List;

public interface IncidentDisciplineService {
    /** Enregistre l'incident et notifie automatiquement le(s) parent(s) de l'élève. */
    IncidentDisciplineResponse enregistrerIncident(IncidentDiscipline incident, Long eleveId, Long classeId, Long classeMatiereId);

    List<IncidentDisciplineResponse> listerParEleve(Long eleveId);

    List<IncidentDisciplineResponse> listerParClasseEtDate(Long classeId, LocalDate date);

    /** Marque la fiche comme suivie par la direction/surveillance. */
    IncidentDisciplineResponse marquerTraite(Long id, String notesTraitement);
}
