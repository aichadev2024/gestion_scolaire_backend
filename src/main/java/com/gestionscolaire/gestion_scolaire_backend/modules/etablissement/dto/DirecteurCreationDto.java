package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto.ProfilDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Un compte DIRECTEUR à créer en même temps que l'établissement. Certaines écoles n'ont qu'un
 * seul directeur pour tout l'établissement (niveauSuperviseId = null) ; d'autres ont un directeur
 * par niveau (ex. un censeur pour le Lycée, un autre directeur pour le Collège) — dans ce cas,
 * la requête contient plusieurs DirecteurCreationDto, un par niveau.
 */
@Data
public class DirecteurCreationDto {

    @NotBlank(message = "Le nom d'utilisateur du directeur est obligatoire")
    private String username;

    private String email;

    @NotBlank(message = "Le mot de passe du directeur est obligatoire")
    private String motDePasse;

    @NotNull(message = "Le profil du directeur est obligatoire")
    @Valid
    private ProfilDto profil;

    /** Niveau auquel restreindre ce compte (ex. Lycée → affiché "Censeur") — null = accès à tout l'établissement ("Directeur"). */
    private Integer niveauSuperviseId;
}
