package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto;

import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.StatutEtablissement;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EtablissementResponse {
    private Long id;
    private String nom;
    private String code;
    private String emailContact;
    private String telephone;
    private String adresse;
    private StatutEtablissement statut;
    private String planTarifaire;
    private LocalDateTime dateExpirationAbonnement;
    private LocalDateTime dateCreation;

    // ── Informations Administrateur Établissement ──
    private String adminUsername;
    private String adminNomComplet;
    private String adminEmail;
}


