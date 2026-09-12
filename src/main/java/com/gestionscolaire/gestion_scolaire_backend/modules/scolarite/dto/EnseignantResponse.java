package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto.ProfilDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnseignantResponse {
    private Long id;
    private String matricule;
    private String biographie;
    private ProfilDto profil;
    /** Vrai si cette personne est professeure principale d'au moins une classe de niveau "Crèche" — dans une
     * école mixte (crèche + primaire + collège...), certains membres du personnel sont des monitrices,
     * d'autres de vrais enseignants ; ce n'est jamais une propriété globale de l'établissement. */
    private Boolean estMonitrice;
    /** Renseigné uniquement dans la réponse de création : mot de passe initial à transmettre à l'enseignant. */
    private String motDePasseInitial;
}


