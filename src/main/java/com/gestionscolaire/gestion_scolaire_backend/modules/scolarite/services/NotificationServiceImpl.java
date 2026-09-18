package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.core.services.PushNotificationService;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Notification;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.NotificationRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard;
    private final ObjectProvider<PushNotificationService> pushNotificationServiceProvider;

    public NotificationServiceImpl(NotificationRepository notificationRepository, UtilisateurRepository utilisateurRepository,
                                   com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard,
                                   ObjectProvider<PushNotificationService> pushNotificationServiceProvider) {
        this.notificationRepository = notificationRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.tenantGuard = tenantGuard;
        this.pushNotificationServiceProvider = pushNotificationServiceProvider;
    }

    @Override
    public Notification envoyerNotification(Notification notification, Long expediteurId, Long destinataireId) {
        Utilisateur destinataire = tenantGuard.requireSameTenant(utilisateurRepository.findById(destinataireId)
                .orElseThrow(() -> new ResourceNotFoundException("Destinataire introuvable")));

        if (expediteurId != null) {
            Utilisateur expediteur = tenantGuard.requireSameTenant(utilisateurRepository.findById(expediteurId)
                    .orElseThrow(() -> new ResourceNotFoundException("Expéditeur introuvable")));
            notification.setExpediteur(expediteur);
        }

        notification.setDestinataire(destinataire);
        notification.setEtablissement(destinataire.getEtablissement());
        notification.setEstLu(false);
        Notification saved = notificationRepository.save(notification);

        // Envoi push best-effort : un échec ici ne doit jamais faire échouer la notification
        // elle-même (déjà persistée et lisible via l'appli) — même philosophie que les try/catch
        // existants autour des envois d'e-mail dans EtablissementServiceImpl.
        try {
            PushNotificationService push = pushNotificationServiceProvider.getIfAvailable();
            if (push != null) {
                push.envoyerPush(destinataire.getId(), saved.getTitre(), saved.getContenu());
            }
        } catch (Exception e) {
            log.warn("Envoi push non effectué pour la notification {} : {}", saved.getId(), e.getMessage());
        }

        return saved;
    }

    @Override
    public void notifierParentsEleve(com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve eleve, String titre, String contenu, Long expediteurId) {
        if (eleve == null) return;
        // Arrays.asList (pas List.of) : la plupart des élèves n'ont pas de parent secondaire (null).
        for (com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Parent parent
                : java.util.Arrays.asList(eleve.getParent(), eleve.getParentSecondaire())) {
            if (parent == null || parent.getProfil() == null || parent.getProfil().getUtilisateur() == null) continue;
            try {
                envoyerNotification(Notification.builder().titre(titre).contenu(contenu).build(),
                        expediteurId, parent.getProfil().getUtilisateur().getId());
            } catch (Exception e) {
                log.warn("Notification « {} » non envoyée au parent {} : {}", titre, parent.getId(), e.getMessage());
            }
        }
    }

    @Override
    public List<Notification> listerPourDestinataire(Long destinataireId) {
        return tenantGuard.filterSameTenant(notificationRepository.findByDestinataireIdOrderByDateCreationDesc(destinataireId));
    }

    @Override
    public List<Notification> listerNonLues(Long destinataireId) {
        return tenantGuard.filterSameTenant(notificationRepository.findByDestinataireIdAndEstLuOrderByDateCreationDesc(destinataireId, false));
    }

    @Override
    public Optional<Notification> trouverParId(Long id) {
        return notificationRepository.findById(id).filter(tenantGuard::appartientAuTenantCourant);
    }

    @Override
    public void marquerCommeLue(Long id) {
        Notification notification = tenantGuard.requireSameTenant(notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable")));
        notification.setEstLu(true);
        notificationRepository.save(notification);
    }
}


