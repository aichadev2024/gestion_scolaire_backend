package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "emplois_du_temps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmploiDuTemps {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classe_matiere_id", nullable = true)
    private ClasseMatiere classeMatiere;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classe_id", nullable = true)
    private Classe classe;

    @Column(name = "type_creneau", length = 30)
    @Builder.Default
    private String typeCreneau = "COURS"; // COURS, RECREATION, DEJEUNER, PAUSE

    @Column(name = "libelle_pause", length = 100)
    private String libellePause;

    @Column(name = "jour_semaine", nullable = false)
    private Integer jourSemaine; // 1 = Lundi, 7 = Dimanche

    @Column(name = "heure_debut", nullable = false)
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm:ss")
    private LocalTime heureDebut;

    @Column(name = "heure_fin", nullable = false)
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm:ss")
    private LocalTime heureFin;

    @Column(length = 50)
    private String salle;
}


