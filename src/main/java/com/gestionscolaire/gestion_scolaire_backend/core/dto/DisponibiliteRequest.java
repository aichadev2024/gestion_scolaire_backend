package com.gestionscolaire.gestion_scolaire_backend.core.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class DisponibiliteRequest {
    @NotNull(message = "Le jour de la semaine est obligatoire")
    @Min(value = 1, message = "Le jour de la semaine doit être entre 1 (lundi) et 6 (samedi)")
    @Max(value = 6, message = "Le jour de la semaine doit être entre 1 (lundi) et 6 (samedi)")
    private Integer jourSemaine;

    @NotNull(message = "L'heure de début est obligatoire")
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm:ss")
    private LocalTime heureDebut;

    @NotNull(message = "L'heure de fin est obligatoire")
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm:ss")
    private LocalTime heureFin;

    /** DISPONIBLE ou INDISPONIBLE — validé côté service. */
    private String type;

    private String commentaire;
}
