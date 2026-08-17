package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulletinResponse {
    private Long id;
    private Long eleveId;
    private String eleveNom;
    private String elevePrenom;
    private String eleveMatricule;
    private Long classeId;
    private String classeNom;
    private String periode;
    private String anneeScolaire;
    private Double moyenneGenerale;
    private String appreciationGenerale;
    private Boolean estVerrouille;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private List<BulletinLigneResponse> lignes;
}


