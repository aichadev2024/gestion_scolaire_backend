package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Situation financière d'un élève pour l'année : ce qui est dû frais par frais (inscription,
 * mensualités…), ce qui est payé, et l'historique des reçus avec l'objet de chaque paiement.
 */
public record SituationFinanciereResponse(
        String devise,
        double totalDu,
        double totalPaye,
        double reste,
        boolean aucunFraisDefini,
        boolean toutPaye,
        double creditNonUtilise,
        List<LigneFrais> lignes,
        List<PaiementRecu> paiements
) {
    /** type : INSCRIPTION, MENSUALITE ou AUTRE — statut : PAYE, PARTIEL, A_PAYER ou EN_RETARD. */
    public record LigneFrais(Long fraisId, String titre, String type, double montant, double paye,
                             double reste, LocalDate dateEcheance, String statut) {}

    public record PaiementRecu(Long id, String numeroRecu, double montant, LocalDateTime date,
                               String mode, String objet) {}
}
