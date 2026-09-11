package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models;

import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantEntityListener;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantScoped;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "eleves")
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Eleve implements TenantScoped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "profil_id", unique = true, nullable = false)
    private Profil profil;

    @Column(nullable = false, unique = true, length = 50)
    private String matricule;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classe_id")
    private Classe classe;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    private Parent parent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_secondaire_id")
    private Parent parentSecondaire;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @Builder.Default
    @Column(length = 20)
    private String statut = "ACTIF";

    /**
     * Statut du dossier d'inscription : VALIDEE, EN_ATTENTE ou ANNULEE.
     * Distinct de {@link #statut} (ACTIF/ARCHIVE, qui gouverne la carte
     * scolaire et l'accès) — un dossier peut être « en attente » de pièces
     * ou de paiement sans que le compte ne soit désactivé pour autant.
     */
    @Builder.Default
    @Column(name = "statut_inscription", length = 20)
    private String statutInscription = "VALIDEE";

    /** Non persisté : mot de passe initial généré à l'inscription, renvoyé une seule fois à l'admin. */
    @Transient
    private String motDePasseInitial;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}


