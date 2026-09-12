package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "etablissements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Etablissement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // Ex: "jules-verne", "excellence"

    @Column(name = "email_contact", length = 100)
    private String emailContact;

    @Column(length = 30)
    private String telephone;

    @Column(length = 255)
    private String adresse;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(length = 10)
    @Builder.Default
    private String devise = "FCFA";

    /** Devise au sens « slogan » de l'école, ex. « Travail - Rigueur - Réussite ». */
    @Column(length = 255)
    private String slogan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatutEtablissement statut = StatutEtablissement.ACTIF;

    /** École (par défaut) ou Crèche — fixé à la création, oriente le vocabulaire côté web (Monitrices, Groupes…). */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_etablissement", nullable = false, length = 20)
    @Builder.Default
    private TypeEtablissement typeEtablissement = TypeEtablissement.ECOLE;

    @Column(name = "plan_tarifaire", length = 50)
    @Builder.Default
    private String planTarifaire = "STANDARD";

    @Column(name = "date_expiration_abonnement")
    private LocalDateTime dateExpirationAbonnement;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;
}


