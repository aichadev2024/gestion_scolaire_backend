package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models;

import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantEntityListener;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantScoped;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Sujet de devoir ou d'examen qu'un enseignant transmet à la direction avant de le donner aux
 * élèves — contrôle avant diffusion. Le fichier (photo ou PDF) est stocké sur R2, seule l'URL
 * est gardée ici (même schéma que {@link DocumentEleve}).
 */
@Entity
@Table(name = "sujets_devoirs")
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SujetDevoir implements TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classe_matiere_id", nullable = false)
    private ClasseMatiere classeMatiere;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "enseignant_id", nullable = false)
    private Enseignant enseignant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    /** DEVOIR ou EXAMEN. */
    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "taille_octets")
    private Long tailleOctets;

    /** EN_ATTENTE, VALIDE ou REJETE. */
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String statut = "EN_ATTENTE";

    @Column(name = "commentaire_direction", columnDefinition = "TEXT")
    private String commentaireDirection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traite_par_id")
    private Utilisateur traitePar;

    @CreationTimestamp
    @Column(name = "date_envoi", nullable = false, updatable = false)
    private LocalDateTime dateEnvoi;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;
}
