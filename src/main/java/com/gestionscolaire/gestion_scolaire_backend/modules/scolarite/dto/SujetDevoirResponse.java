package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto;

import java.time.LocalDateTime;

/** type : DEVOIR ou EXAMEN — statut : EN_ATTENTE, VALIDE ou REJETE. */
public record SujetDevoirResponse(
        Long id,
        Long classeMatiereId,
        String classeNom,
        String matiereNom,
        String enseignantNom,
        String type,
        String titre,
        String description,
        String url,
        String contentType,
        Long tailleOctets,
        String statut,
        String commentaireDirection,
        String traitePar,
        LocalDateTime dateEnvoi,
        LocalDateTime dateTraitement
) {}
