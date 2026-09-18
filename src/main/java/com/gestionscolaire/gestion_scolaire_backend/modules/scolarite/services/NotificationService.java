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
}


