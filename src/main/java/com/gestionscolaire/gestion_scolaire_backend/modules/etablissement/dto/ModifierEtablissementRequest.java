package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Coordonnées d'un établissement — modifiables après création (pas le plan ni l'expiration, gérés par /renouveler). */
@Data
public class ModifierEtablissementRequest {

    @NotBlank(message = "Le nom de l'établissement est obligatoire")
    private String nom;

    private String emailContact;
    private String telephone;
    private String adresse;
    private String devise;
}
