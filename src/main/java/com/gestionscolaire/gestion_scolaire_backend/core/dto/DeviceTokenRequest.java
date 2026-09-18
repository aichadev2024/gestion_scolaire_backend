package com.gestionscolaire.gestion_scolaire_backend.core.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceTokenRequest {
    @NotBlank(message = "Le token est obligatoire")
    private String token;

    @NotBlank(message = "La plateforme est obligatoire")
    private String plateforme; // ANDROID, WEB
}
