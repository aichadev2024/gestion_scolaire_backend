package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models;

import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantEntityListener;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantScoped;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Document du dossier d'inscription d'un élève (acte de naissance, certificat
 * médical, etc.) — le fichier lui-même est stocké sur R2 (voir
 * {@code core.storage}), seule l'URL est gardée ici.
 */
@Entity
@Table(name = "documents_eleve")
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentEleve implements TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eleve_id", nullable = false)
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    /** ACTE_NAISSANCE, CERTIFICAT_MEDICAL ou AUTRE — sert surtout à choisir une icône côté client. */
    @Column(nullable = false, length = 30)
    private String type;

    /** Titre lisible du document (ex. « Acte de naissance », « Certificat médical 2026 »). */
    @Column(nullable = false, length = 150)
    private String libelle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "taille_octets")
    private Long tailleOctets;

    @CreationTimestamp
    @Column(name = "date_ajout", nullable = false, updatable = false)
    private LocalDateTime dateAjout;
}
