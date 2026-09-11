package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Un élève dont au moins une échéance de frais de scolarité est dépassée et non couverte. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetardPaiementResponse {
    private Long eleveId;
    private String eleveNom;
    private String elevePrenom;
    private String matricule;
    private Long classeId;
    private String classeNom;
    private String parentNom;
    private String parentPrenom;
    private String parentTelephone;
    private Double montantDu;
    private LocalDate echeanceLaPlusAncienne;
    private Long joursRetard;
}
