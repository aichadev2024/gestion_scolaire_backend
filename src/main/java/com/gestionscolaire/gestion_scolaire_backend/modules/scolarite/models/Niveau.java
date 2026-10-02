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
    /** Niveau standard du catalogue pour l'enseignement professionnel/technique. Chaque école peut
     * lui donner son propre nom (« IFTICA » pour le lycée Ba Fanta Coulibaly…) — voir {@link #libelle}. */
    public static final String ENSEIGNEMENT_PROFESSIONNEL = "Enseignement Professionnel";

    /**
     * Nom à AFFICHER pour ce niveau dans l'établissement donné. Seul l'enseignement professionnel
     * porte un nom propre à l'école ; tous les autres niveaux gardent leur nom du catalogue. C'est un
     * nom d'affichage uniquement : la logique métier continue d'utiliser le nom du catalogue.
     */
    public static String libelle(Niveau niveau, com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement etablissement) {
        if (niveau == null) return null;
        if (etablissement != null && ENSEIGNEMENT_PROFESSIONNEL.equalsIgnoreCase(niveau.getNom())) {
            String perso = etablissement.getNomEnseignementProfessionnel();
            if (perso != null && !perso.isBlank()) return perso;
        }
        return niveau.getNom();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String nom;
}


