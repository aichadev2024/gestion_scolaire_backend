package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto.ProfilDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateEtablissementWithAdminRequest {

    // ── Informations de l'établissement ──────────────────────────
    @NotBlank(message = "Le nom de l'établissement est obligatoire")
    private String nomEtablissement;

    @NotBlank(message = "Le code de l'établissement (ex: jules-verne) est obligatoire")
    private String codeEtablissement;

    private String emailContact;
    private String telephone;
    private String adresse;
    private String planTarifaire;
    private LocalDateTime dateExpirationAbonnement;

    // ── Premier administrateur de l'école ─────────────────────────
    @NotBlank(message = "Le nom d'utilisateur de l'administrateur est obligatoire")
    private String adminUsername;

    private String adminEmail;

    @NotBlank(message = "Le mot de passe de l'administrateur est obligatoire")
    private String adminMotDePasse;

    @NotNull(message = "Le profil de l'administrateur est obligatoire")
    @Valid
    private ProfilDto adminProfil;
}


