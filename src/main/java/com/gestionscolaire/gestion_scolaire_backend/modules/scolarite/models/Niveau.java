package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "niveaux")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Niveau {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String nom;
}


