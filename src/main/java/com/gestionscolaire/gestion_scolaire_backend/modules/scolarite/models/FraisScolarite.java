package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "frais_scolarite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraisScolarite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classe_id", nullable = false)
    private Classe classe;

    @Column(nullable = false, length = 100)
    private String titre;

    @Column(nullable = false)
    private Double montant;

    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}


