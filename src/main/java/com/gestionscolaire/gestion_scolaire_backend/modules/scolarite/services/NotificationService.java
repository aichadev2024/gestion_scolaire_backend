package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationService {
    Notification envoyerNotification(Notification notification, Long expediteurId, Long destinataireId);
    /** Notifie (base + push) le parent principal et le parent secondaire de l'élève, sans jamais lever d'erreur. */
    void notifierParentsEleve(com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve eleve, String titre, String contenu, Long expediteurId);
    List<Notification> listerPourDestinataire(Long destinataireId);
    List<Notification> listerNonLues(Long destinataireId);
    Optional<Notification> trouverParId(Long id);
    void marquerCommeLue(Long id);
    /** Supprime une notification — réservé à son propre destinataire (vérifié via l'utilisateur connecté). */
    void supprimer(Long id);
    /** Supprime toutes les notifications de l'utilisateur connecté (nettoyage en une fois). */
    void supprimerToutes();
    /**
     * Réponse du destinataire à une notification (justification ou non) — réservé à son propre
     * destinataire. Envoie en retour une nouvelle notification (avec push) à l'expéditeur d'origine,
     * s'il y en a un.
     */
    Notification repondre(Long id, String contenu);
}


