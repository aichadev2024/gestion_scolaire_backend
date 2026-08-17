package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Classe;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bulletins", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"eleve_id", "periode", "annee_scolaire"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bulletin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "eleve_id", nullable = false)
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classe_id", nullable = false)
    private Classe classe;

    @Column(nullable = false, length = 20)
    private String periode; // TRIMESTRE_1, TRIMESTRE_2, TRIMESTRE_3

    @Column(name = "annee_scolaire", nullable = false, length = 20)
    private String anneeScolaire;

    @Column(name = "moyenne_generale")
    private Double moyenneGenerale;

    @Column(name = "appreciation_generale", columnDefinition = "TEXT")
    private String appreciationGenerale;

    @Column(name = "est_verrouille", nullable = false)
    @Builder.Default
    private Boolean estVerrouille = false;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;
}


