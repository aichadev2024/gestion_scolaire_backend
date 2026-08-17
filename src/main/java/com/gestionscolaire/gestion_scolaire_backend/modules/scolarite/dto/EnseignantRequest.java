package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto.ProfilDto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnseignantRequest {
    @NotNull(message = "Le profil est obligatoire")
    private ProfilDto profil;
    private String biographie;
}


