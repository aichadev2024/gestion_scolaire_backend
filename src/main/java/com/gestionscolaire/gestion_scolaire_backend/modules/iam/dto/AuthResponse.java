package com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type;
    private Long utilisateurId;
    private String email;
    private String username;
    private String role;
    private String prenom;
    private String nom;
    private Long etablissementId;
    private String etablissementNom;
    private String etablissementLogoUrl;
    private String etablissementDevise;
    private String etablissementSlogan;
    private String etablissementType;
    /** Vrai si l'établissement a au moins une classe de niveau "Crèche" — indépendant de etablissementType,
     * pour couvrir les écoles hybrides (crèche + maternelle + primaire dans le même établissement). */
    private Boolean aClassesCreche;
    /** Vrai si TOUTES les classes de l'établissement sont de niveau "Crèche" (aucun primaire/collège/lycée à
     * côté) — sert uniquement à choisir le libellé générique du menu ("Monitrices" vs "Enseignants") dans une
     * école mixte, où le personnel réel est un mélange des deux. */
    private Boolean etablissementUniquementCreche;
    /** Vrai si CET utilisateur (role ENSEIGNANT) est professeur principal d'au moins une classe de niveau
     * "Crèche" — propre à la personne, pas à l'établissement : dans une école mixte, seuls certains membres
     * du personnel sont des monitrices. */
    private Boolean estMonitrice;
    private String etablissementPlanTarifaire;
    private Integer etablissementMaxEnseignants;
    private Long eleveId;
    private String classeNom;
    private Boolean requiresOtp;
    private String message;
}


