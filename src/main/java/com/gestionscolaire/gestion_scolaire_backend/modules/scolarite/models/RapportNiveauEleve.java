package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models;

import jakarta.persistence.*;
import lombok.*;

/** Un élève signalé en difficulté dans un rapport de niveau (accès uniquement via son rapport). */
@Entity
@Table(name = "rapports_niveau_eleves")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RapportNiveauEleve {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rapport_id", nullable = false)
    private RapportNiveau rapport;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "eleve_id", nullable = false)
    private Eleve eleve;

    /** Moyenne de l'élève dans la matière sur la période, figée au moment du rapport. */
    private Double moyenne;

    @Column(length = 500)
    private String commentaire;
}
