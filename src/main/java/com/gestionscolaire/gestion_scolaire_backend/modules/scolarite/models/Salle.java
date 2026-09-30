package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models;

import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantEntityListener;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantScoped;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

/** Salle de classe nommée (catalogue optionnel) — voir V26__salles.sql. */
@Entity
@Table(name = "salles")
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Salle implements TenantScoped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    private Integer capacite;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
