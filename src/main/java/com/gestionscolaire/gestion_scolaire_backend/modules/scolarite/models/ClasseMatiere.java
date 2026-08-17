package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "classes_matieres", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"classe_id", "matiere_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClasseMatiere {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classe_id", nullable = false)
    private Classe classe;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "matiere_id", nullable = false)
    private Matiere matiere;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "enseignant_id")
    private Enseignant enseignant;

    @Builder.Default
    @Column(nullable = false)
    private Double coefficient = 1.0;
}


