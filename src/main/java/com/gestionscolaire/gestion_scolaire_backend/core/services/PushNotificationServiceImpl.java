package com.gestionscolaire.gestion_scolaire_backend.core.services;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.DeviceToken;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.DeviceTokenRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;

/**
 * Envoie les notifications push via Firebase Cloud Messaging. Bean créé uniquement si
 * {@code push.fcm.enabled=true} — injecté via {@code ObjectProvider} dans
 * {@code NotificationServiceImpl}, comme {@code StorageService} l'est déjà dans
 * {@code ProfilPhotoService} : sans variable d'environnement posée, aucun bean n'existe et
 * l'appelant continue sans push, sans erreur.
 */
@Service
@ConditionalOnProperty(prefix = "push.fcm", name = "enabled", havingValue = "true")
public class PushNotificationServiceImpl implements PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationServiceImpl.class);

    @Value("${push.fcm.service-account-b64}")
    private String serviceAccountB64;

    private final DeviceTokenRepository deviceTokenRepository;

    private FirebaseApp firebaseApp;

    public PushNotificationServiceImpl(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @PostConstruct
    private void init() {
        try {
            byte[] jsonBytes = Base64.getDecoder().decode(serviceAccountB64);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(jsonBytes)))
                    .build();
            firebaseApp = FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
            log.info("Firebase Cloud Messaging initialisé.");
        } catch (Exception e) {
            log.error("Échec d'initialisation de Firebase (push.fcm.enabled=true mais configuration invalide) : {}", e.getMessage());
        }
    }

    @Override
    public void envoyerPush(Long utilisateurId, String titre, String contenu) {
        if (firebaseApp == null) return; // init() a échoué : pas de push, pas d'erreur bloquante

        List<DeviceToken> tokens = deviceTokenRepository.findByUtilisateurId(utilisateurId);
        for (DeviceToken deviceToken : tokens) {
            Message message = Message.builder()
                    .setToken(deviceToken.getToken())
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(titre)
                            .setBody(contenu)
                            .build())
                    .build();
            try {
                FirebaseMessaging.getInstance(firebaseApp).send(message);
            } catch (FirebaseMessagingException e) {
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                        || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                    log.info("Token FCM invalide/désinstallé, suppression : utilisateur {}", utilisateurId);
                    deviceTokenRepository.deleteByToken(deviceToken.getToken());
                } else {
                    log.warn("Échec d'envoi push à l'utilisateur {} : {}", utilisateurId, e.getMessage());
                }
            } catch (Exception e) {
                log.warn("Échec d'envoi push à l'utilisateur {} : {}", utilisateurId, e.getMessage());
            }
        }
    }
}
