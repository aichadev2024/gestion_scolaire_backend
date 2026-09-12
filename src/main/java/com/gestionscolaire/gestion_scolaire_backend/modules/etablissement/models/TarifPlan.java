package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "tarifs_plan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarifPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "prix_mensuel", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixMensuel;

    /** Nombre max de comptes enseignants pour ce plan — null = illimité. */
    @Column(name = "max_enseignants")
    private Integer maxEnseignants;

    @UpdateTimestamp
    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;
}
