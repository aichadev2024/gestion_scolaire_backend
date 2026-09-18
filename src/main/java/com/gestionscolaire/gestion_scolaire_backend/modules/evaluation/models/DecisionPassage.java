package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models;

import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantEntityListener;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantScoped;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "decisions_passage", uniqueConstraints = @UniqueConstraint(columnNames = {"eleve_id", "annee_scolaire"}))
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DecisionPassage implements TenantScoped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eleve_id", nullable = false)
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decide_par_id")
    private Utilisateur decidePar;

    @Column(name = "annee_scolaire", nullable = false, length = 20)
    private String anneeScolaire;

    @Column(nullable = false, length = 20)
    private String decision; // PASSAGE, REDOUBLEMENT

    @Column(length = 500)
    private String commentaire;

    @Column(name = "date_decision", nullable = false)
    private LocalDateTime dateDecision;
}
