package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models;

import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantEntityListener;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantScoped;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Disponibilité (ou indisponibilité) qu'un enseignant déclare pour un jour/créneau —
 * consultée par la direction pour construire l'emploi du temps. Voir
 * V27__disponibilites_enseignant.sql.
 */
@Entity
@Table(name = "disponibilites_enseignant")
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisponibiliteEnseignant implements TenantScoped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "enseignant_id", nullable = false)
    private Enseignant enseignant;

    @Column(name = "jour_semaine", nullable = false)
    private Integer jourSemaine;

    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @Column(name = "heure_fin", nullable = false)
    private LocalTime heureFin;

    /** DISPONIBLE ou INDISPONIBLE. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String type = "DISPONIBLE";

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
