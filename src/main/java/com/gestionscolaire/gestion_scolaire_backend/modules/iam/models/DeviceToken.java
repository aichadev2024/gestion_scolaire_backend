package com.gestionscolaire.gestion_scolaire_backend.modules.iam.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Token FCM (Firebase Cloud Messaging) d'un appareil/navigateur — un utilisateur peut en avoir
 * plusieurs (téléphone Android + navigateur web). Entité séparée plutôt qu'une collection sur
 * {@link Utilisateur} (pas de relation OneToMany ailleurs dans ce codebase). */
@Entity
@Table(name = "device_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;

    @Column(nullable = false, length = 20)
    private String plateforme; // ANDROID, WEB

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_derniere_utilisation", nullable = false)
    private LocalDateTime dateDerniereUtilisation;
}
