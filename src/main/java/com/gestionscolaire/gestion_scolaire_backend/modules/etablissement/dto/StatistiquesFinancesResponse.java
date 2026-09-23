package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Détail des finances de l'établissement pour l'espace promoteur/direction : ce qui a été
 * encaissé ce mois-ci et cette année, la courbe des 12 derniers mois, et le détail de chaque
 * paiement (élève, frais concerné) pour une lecture complète, pas seulement des totaux.
 */
public record StatistiquesFinancesResponse(
        String devise,
        double totalFraisAttendus,
        double totalEncaisse,
        double soldeRestant,
        double encaisseMoisCourant,
        double encaisseAnneeCourante,
        List<MoisMontant> parMois,
        List<PaiementDetail> paiements
) {
    /** Un point de la courbe des 12 derniers mois. */
    public record MoisMontant(String mois, String libelle, double montant) {}

    /** type : INSCRIPTION, MENSUALITE ou AUTRE — une ligne du relevé détaillé des paiements. */
    public record PaiementDetail(
            Long id,
            Long eleveId,
            String eleveNom,
            String elevePrenom,
            String matricule,
            String classeNom,
            String fraisTitre,
            String type,
            double montant,
            LocalDateTime date,
            String mode,
            String numeroRecu
    ) {}
}
