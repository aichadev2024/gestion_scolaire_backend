package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PromotionRequest {
    @NotNull(message = "La classe de destination est obligatoire")
    private Long classeDestinationId;

    @NotEmpty(message = "Sélectionnez au moins un élève à faire passer")
    private List<Long> eleveIds;
}
