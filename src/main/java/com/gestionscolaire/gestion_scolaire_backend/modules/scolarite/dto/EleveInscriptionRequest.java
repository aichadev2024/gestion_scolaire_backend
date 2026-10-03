package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto.ProfilDto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EleveInscriptionRequest {
    @NotNull(message = "Le profil est obligatoire")
    private ProfilDto profil;
    private Long parentId;
    private Long classeId;

    /** Arriérés des années précédentes (FCFA). À la modification : absent = inchangé, 0 = effacer. */
    @jakarta.validation.constraints.PositiveOrZero(message = "Le montant des arriérés ne peut pas être négatif")
    private Double arrieresMontant;

    @jakarta.validation.constraints.Size(max = 100, message = "L'intitulé des arriérés est limité à 100 caractères")
    private String arrieresLibelle;
}


