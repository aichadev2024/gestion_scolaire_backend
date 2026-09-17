package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateEtablissementWithAdminRequest {

    // ── Informations de l'établissement ──────────────────────────
    @NotBlank(message = "Le nom de l'établissement est obligatoire")
    private String nomEtablissement;

    private String codeEtablissement;

    private String emailContact;
    private String telephone;
    private String adresse;
    private String planTarifaire;
    private LocalDateTime dateExpirationAbonnement;
    /** ECOLE (défaut) ou CRECHE. */
    private String typeEtablissement;

    /** Comptes DIRECTEUR à créer avec l'établissement — un seul (accès à tout) pour la plupart des
     * écoles, ou plusieurs (un par niveau, ex. Censeur du Lycée + Directeur du Collège) pour les
     * établissements organisés ainsi. Au moins un est obligatoire. */
    @NotEmpty(message = "Au moins un directeur est obligatoire")
    @Valid
    private List<DirecteurCreationDto> directeurs;
}
