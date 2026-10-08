package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto.RapportTrimestrielRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto.RapportTrimestrielResponse;

import java.time.LocalDate;

public interface RapportTrimestrielService {
    /**
     * Calcule le rapport d'une classe ({@code classeId}) ou de tout l'établissement (null). Les dates
     * servent aux présences et à la discipline ; absentes, on prend celles enregistrées, sinon le
     * calendrier habituel du trimestre.
     */
    RapportTrimestrielResponse generer(Long classeId, String periode, String anneeScolaire, LocalDate debut, LocalDate fin);

    /** Enregistre le commentaire (bilan) de la direction et les dates retenues pour ce rapport. */
    void enregistrer(RapportTrimestrielRequest request);
}
