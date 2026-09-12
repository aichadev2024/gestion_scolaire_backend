package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models;

import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantEntityListener;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantScoped;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Rapport journalier d'un enfant (repas, sieste, couches, humeur) — usage
 * typique en crèche, mais pas restreint : un seul rapport par (élève, date).
 */
@Entity
@Table(name = "rapports_journaliers")
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RapportJournalier implements TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "eleve_id", nullable = false)
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @Column(nullable = false)
    private LocalDate date;

    /** Ex. "Bien mangé", "Peu mangé", "N'a rien mangé" — texte libre. */
    @Column(length = 50)
    private String repas;

    @Column(name = "sieste_faite")
    private Boolean siesteFaite;

    @Column(name = "duree_sieste_minutes")
    private Integer dureeSiesteMinutes;

    @Column(name = "changes_couches")
    private Integer changesCouches;

    /** Ex. "Joyeux", "Calme", "Agité", "Fatigué", "Malade" — texte libre. */
    @Column(length = 50)
    private String humeur;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "redige_par_id")
    private Utilisateur redigePar;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;
}
