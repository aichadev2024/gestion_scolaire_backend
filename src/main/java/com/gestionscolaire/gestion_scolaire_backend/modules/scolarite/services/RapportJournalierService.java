package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.RapportJournalierRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.RapportJournalier;

import java.time.LocalDate;
import java.util.List;

public interface RapportJournalierService {
    /** Crée le rapport du jour pour cet élève, ou met à jour celui déjà existant (un seul par élève/date). */
    RapportJournalier enregistrer(RapportJournalierRequest request);
    List<RapportJournalier> listerParEleve(Long eleveId);
    List<RapportJournalier> listerParClasseEtDate(Long classeId, LocalDate date);
}
