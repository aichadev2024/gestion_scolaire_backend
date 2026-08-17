package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.ClasseMatiere;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "eleve_id", nullable = false)
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classe_matiere_id", nullable = false)
    private ClasseMatiere classeMatiere;

    @Column(nullable = false, length = 20)
    private String periode; // ex: TRIMESTRE_1, SEMESTRE_1

    @Column(name = "type_evaluation", nullable = false, length = 50)
    private String typeEvaluation; // ex: DEVOIR, EXAMEN

    @Column(nullable = false)
    private Double valeur; // Note de 0 à 20

    @Builder.Default
    @Column(name = "note_max")
    private Double noteMax = 20.0;

    @Column(columnDefinition = "TEXT")
    private String appreciation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par")
    private Utilisateur creePar;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;
}


