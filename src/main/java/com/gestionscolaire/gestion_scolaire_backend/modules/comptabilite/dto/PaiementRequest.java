package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaiementRequest {
    @NotNull(message = "L'élève est obligatoire")
    private Long eleveId;

    private Long fraisId;

    @NotNull(message = "Le montant est obligatoire")
    private Double montantPaye;

    @NotBlank(message = "Le mode de paiement est obligatoire")
    private String modePaiement;

    private String referenceTransaction;
}


