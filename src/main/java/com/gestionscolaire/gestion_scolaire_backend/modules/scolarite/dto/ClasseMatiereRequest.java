package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClasseMatiereRequest {

    @NotNull
    private Long classeId;

    @NotNull
    private Long matiereId;

    private Long enseignantId;

    @Positive
    private Double coefficient = 1.0;
}


