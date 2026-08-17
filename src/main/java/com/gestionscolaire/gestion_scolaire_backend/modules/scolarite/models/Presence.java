package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "presences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Presence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "eleve_id", nullable = false)
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classe_matiere_id")
    private ClasseMatiere classeMatiere;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 20)
    private String statut; // PRESENT, ABSENT, RETARD

    @Builder.Default
    @Column(name = "est_justifie")
    private Boolean estJustifie = false;

    @Column(name = "notes_justification", columnDefinition = "TEXT")
    private String notesJustification;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}


