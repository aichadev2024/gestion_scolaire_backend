package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models;

import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantEntityListener;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantScoped;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "incidents_discipline")
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentDiscipline implements TenantScoped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "eleve_id", nullable = false)
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classe_id", nullable = false)
    private Classe classe;

    /** Cours identifié (classe+matière+enseignant) si l'incident a eu lieu pendant un cours précis —
     * null pour un incident hors-cours (ex. tenue non portée observée dans la cour). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classe_matiere_id")
    private ClasseMatiere classeMatiere;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    /** Auteur de la fiche (surveillant, directeur, secrétaire) — jamais saisi côté client. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "enregistre_par_id")
    private Utilisateur enregistrePar;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime heure;

    @Column(nullable = false, length = 30)
    private String statut; // RETARD, ABSENT, TENUE_NON_PORTEE, REFUS_EXERCICE

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Builder.Default
    @Column(name = "est_traite", nullable = false)
    private Boolean estTraite = false;

    @Column(name = "notes_traitement", columnDefinition = "TEXT")
    private String notesTraitement;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
