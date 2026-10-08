package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto;

import lombok.Data;

import java.util.List;

@Data
public class RelancePaiementRequest {
    /** Élèves à relancer ; absent ou vide = tous les élèves en retard. */
    private List<Long> eleveIds;
}
