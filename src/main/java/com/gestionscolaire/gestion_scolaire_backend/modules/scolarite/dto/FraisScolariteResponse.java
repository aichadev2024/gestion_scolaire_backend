package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraisScolariteResponse {
    private Long id;
    private Long classeId;
    private String classeNom;
    private String titre;
    private Double montant;
    private LocalDate dateEcheance;
}


