package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClasseRequest {
    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotNull(message = "Le niveau est obligatoire")
    private Integer niveauId;

    private Long enseignantPrincipalId;

    @NotBlank(message = "L'année scolaire est obligatoire")
    private String anneeScolaire;

    private Integer capaciteMax = 40;
}


