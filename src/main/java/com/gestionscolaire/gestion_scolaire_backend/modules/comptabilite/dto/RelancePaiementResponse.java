package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto;

/**
 * Bilan d'une relance : {@code envoyees} élèves dont un parent a été notifié, {@code sansCompteParent}
 * ceux dont aucun parent n'a de compte (à appeler), {@code dejaRelances} ceux relancés trop récemment.
 */
public record RelancePaiementResponse(int envoyees, int sansCompteParent, int dejaRelances) {
}
