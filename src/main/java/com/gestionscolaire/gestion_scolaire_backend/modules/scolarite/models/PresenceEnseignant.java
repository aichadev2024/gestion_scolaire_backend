package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "presences_enseignants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresenceEnseignant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "enseignant_id", nullable = false)
    private Enseignant enseignant;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 20)
    private String statut; // PRESENT, ABSENT, RETARD, CONGE

    @Column(name = "heure_arrivee", length = 10)
    private String heureArrivee; // Ex: "07:50"

    @Column(name = "heure_depart", length = 10)
    private String heureDepart; // Ex: "17:00"

    @Column(columnDefinition = "TEXT")
    private String remarques;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
