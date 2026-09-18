package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models;

import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantEntityListener;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantScoped;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Rapport d'un enseignant à la direction sur le niveau d'une classe dans une matière. */
@Entity
@Table(name = "rapports_niveau")
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RapportNiveau implements TenantScoped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classe_matiere_id", nullable = false)
    private ClasseMatiere classeMatiere;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id")
    private Utilisateur auteur;

    @Column(nullable = false, length = 30)
    private String periode;

    /** BON, MOYEN, FAIBLE ou PREOCCUPANT. */
    @Column(name = "niveau_global", nullable = false, length = 20)
    private String niveauGlobal;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Builder.Default
    @Column(name = "est_traite", nullable = false)
    private Boolean estTraite = false;

    @Column(name = "reponse_direction", columnDefinition = "TEXT")
    private String reponseDirection;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;
}
