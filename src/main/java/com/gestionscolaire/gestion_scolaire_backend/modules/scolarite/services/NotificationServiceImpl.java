package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Notification;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.NotificationRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository, UtilisateurRepository utilisateurRepository) {
        this.notificationRepository = notificationRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    public Notification envoyerNotification(Notification notification, Long expediteurId, Long destinataireId) {
        Utilisateur destinataire = utilisateurRepository.findById(destinataireId)
                .orElseThrow(() -> new ResourceNotFoundException("Destinataire introuvable"));

        if (expediteurId != null) {
            Utilisateur expediteur = utilisateurRepository.findById(expediteurId)
                    .orElseThrow(() -> new ResourceNotFoundException("Expéditeur introuvable"));
            notification.setExpediteur(expediteur);
        }

        notification.setDestinataire(destinataire);
        notification.setEstLu(false);
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> listerPourDestinataire(Long destinataireId) {
        return notificationRepository.findByDestinataireIdOrderByDateCreationDesc(destinataireId);
    }

    @Override
    public List<Notification> listerNonLues(Long destinataireId) {
        return notificationRepository.findByDestinataireIdAndEstLuOrderByDateCreationDesc(destinataireId, false);
    }

    @Override
    public Optional<Notification> trouverParId(Long id) {
        return notificationRepository.findById(id);
    }

    @Override
    public void marquerCommeLue(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable"));
        notification.setEstLu(true);
        notificationRepository.save(notification);
    }
}


