package com.gestionscolaire.gestion_scolaire_backend.core.services;

public interface PushNotificationService {
    /** Envoie une notification push à tous les appareils enregistrés de cet utilisateur.
     * N'échoue jamais bruyamment — les erreurs sont journalisées, jamais remontées à l'appelant. */
    void envoyerPush(Long utilisateurId, String titre, String contenu);
}
