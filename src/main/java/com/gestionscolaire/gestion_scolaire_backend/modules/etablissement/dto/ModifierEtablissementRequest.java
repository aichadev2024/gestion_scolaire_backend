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
    /** Slogan de l'école, ex. « Travail - Rigueur - Réussite ». */
    private String slogan;

    /** Niveaux que cet établissement propose. Sémantique volontairement distincte de "vide" :
     * champ absent (null) = ne pas toucher aux niveaux actuels ; liste vide [] explicite = supprimer
     * toute restriction ; liste non vide = remplace intégralement les niveaux autorisés. */
    private java.util.List<Integer> niveauIds;
}
