package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantEntityListener;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantScoped;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "articles_stock")
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleStock implements TenantScoped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 80)
    private String categorie;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String unite = "unité";

    @Builder.Default
    @Column(name = "seuil_alerte", nullable = false)
    private Integer seuilAlerte = 0;

    /** Quantité en stock — modifiée uniquement via un mouvement (entrée/sortie). */
    @Builder.Default
    @Column(nullable = false)
    private Integer quantite = 0;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    /** Vrai si le stock a atteint ou franchi le seuil d'alerte (rupture incluse). */
    public boolean isEnAlerte() {
        return quantite <= seuilAlerte;
    }
}
