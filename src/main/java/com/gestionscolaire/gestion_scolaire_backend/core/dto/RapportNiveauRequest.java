package com.gestionscolaire.gestion_scolaire_backend.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RapportNiveauRequest {
    @NotNull(message = "La classe et la matière sont obligatoires")
    private Long classeMatiereId;

    @NotBlank(message = "La période est obligatoire")
    private String periode;

    /** BON, MOYEN, FAIBLE ou PREOCCUPANT. */
    @NotBlank(message = "Le niveau général de la classe est obligatoire")
    private String niveauGlobal;

    private String commentaire;

    private List<EleveSignale> eleves;

    @Data
    public static class EleveSignale {
        private Long eleveId;
        private String commentaire;
    }
}
