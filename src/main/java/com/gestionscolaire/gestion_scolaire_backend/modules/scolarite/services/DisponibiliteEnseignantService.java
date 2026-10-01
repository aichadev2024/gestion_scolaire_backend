package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.DisponibiliteEnseignant;

import java.util.List;

public interface DisponibiliteEnseignantService {
    /** Crée une disponibilité pour l'enseignant actuellement connecté. */
    DisponibiliteEnseignant creerPourMoi(DisponibiliteEnseignant dispo);
    List<DisponibiliteEnseignant> listerPourMoi();
    /** Réservé à la direction — pour consulter les disponibilités déclarées en construisant l'emploi du temps. */
    List<DisponibiliteEnseignant> listerParEnseignant(Long enseignantId);
    /** Réservé au propriétaire (l'enseignant qui l'a déclarée). */
    void supprimer(Long id);
}
