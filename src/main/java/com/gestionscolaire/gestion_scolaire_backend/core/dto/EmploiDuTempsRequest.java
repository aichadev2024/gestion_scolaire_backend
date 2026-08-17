package com.gestionscolaire.gestion_scolaire_backend.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class EmploiDuTempsRequest {
    private Long classeMatiereId;
    private Long classeId;
    private String typeCreneau; // COURS, RECREATION, DEJEUNER, PAUSE
    private String libellePause;

    @NotNull(message = "Le jour de la semaine est obligatoire")
    private Integer jourSemaine;

    @NotNull(message = "L'heure de début est obligatoire")
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm:ss")
    private LocalTime heureDebut;

    @NotNull(message = "L'heure de fin est obligatoire")
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm:ss")
    private LocalTime heureFin;

    private String salle;
}


