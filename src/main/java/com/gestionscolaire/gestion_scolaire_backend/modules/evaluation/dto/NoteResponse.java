package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteResponse {
    private Long id;
    private Long eleveId;
    private Long classeMatiereId;
    private String periode;
    private String typeEvaluation;
    private Double valeur;
    private Double noteMax;
    private String appreciation;
}


