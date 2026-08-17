package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.FraisScolarite;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "paiements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paiement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "eleve_id", nullable = false)
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "frais_id")
    private FraisScolarite fraisScolarite;

    @Column(name = "montant_paye", nullable = false)
    private Double montantPaye;

    @Column(name = "mode_paiement", nullable = false, length = 50)
    private String modePaiement; // ESPECES, MOBILE_MONEY, VIREMENT, CHEQUE

    @Column(name = "reference_transaction", length = 100)
    private String referenceTransaction;

    @Column(name = "numero_recu", nullable = false, unique = true, length = 100)
    private String numeroRecu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recu_par")
    private Utilisateur recuPar;

    @CreationTimestamp
    @Column(name = "date_paiement", nullable = false, updatable = false)
    private LocalDateTime datePaiement;
}


