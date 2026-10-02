package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Abonnement de l'établissement courant : plan, limite d'élèves et effectif actuel. */
@Data
@Builder
@AllArgsConstructor
public class AbonnementResponse {
    private String plan;
    private String libelle;
    private BigDecimal prixMensuel;
    /** null = illimité. */
    private Integer maxEleves;
    private long elevesActifs;
    private boolean mobileInclus;
    private LocalDateTime dateExpiration;
}
