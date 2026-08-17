package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulletinLigneResponse {
    private Long classeMatiereId;
    private String matiereNom;
    private Double coefficient;
    private Double moyenneEleve;
    private List<NoteDetail> notes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoteDetail {
        private Long id;
        private Double valeur;
        private Double noteMax;
        private String typeEvaluation;
        private String appreciation;
    }
}


