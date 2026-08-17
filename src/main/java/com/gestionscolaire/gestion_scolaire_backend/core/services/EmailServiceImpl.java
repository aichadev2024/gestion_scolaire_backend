package com.gestionscolaire.gestion_scolaire_backend.core.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${spring.mail.username:diarrassoubaa505@gmail.com}")
    private String fromEmail;

    @Override
    public void sendWelcomeEmail(Utilisateur user, String rawPassword) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            logger.warn("Aucun email fourni pour l'utilisateur ID: {}", user.getId());
            return;
        }

        String rawRole = user.getRole() != null ? user.getRole().getNom() : "Utilisateur";
        String roleLabel = switch (rawRole.toUpperCase()) {
            case "SUPER_ADMIN" -> "Administrateur Général (Super-Admin)";
            case "ADMIN" -> "Administrateur d'Établissement";
            case "DIRECTEUR" -> "Directeur d'Établissement";
            case "SECRETAIRE" -> "Secrétaire";
            case "COMPTABLE" -> "Comptable";
            case "ENSEIGNANT" -> "Enseignant";
            case "ELEVE" -> "Élève";
            case "PARENT" -> "Parent d'Élève";
            default -> rawRole;
        };

        boolean isSuperAdmin = "SUPER_ADMIN".equalsIgnoreCase(rawRole);
        boolean hasSchool = user.getEtablissement() != null;
        String headerTitle = hasSchool ? user.getEtablissement().getNom() : "Netaa École — Plateforme Éducative";
        
        String contextSentence;
        if (isSuperAdmin || !hasSchool) {
            contextSentence = "Votre compte <strong>" + roleLabel + "</strong> a été créé avec succès pour la gestion de la plateforme <strong>Netaa École</strong>.";
        } else {
            contextSentence = "Votre compte <strong>" + roleLabel + "</strong> a été créé avec succès pour l'établissement <strong>" + user.getEtablissement().getNom() + "</strong>.";
        }

        String subject = isSuperAdmin 
            ? "🎉 Activation de votre compte Super-Admin Netaa École"
            : "🎉 Bienvenue sur Netaa École - Création de votre compte";
            
        String loginUrl = frontendUrl + "/login";

        String htmlBody = """
            <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 12px; background-color: #ffffff;">
                <div style="text-align: center; padding-bottom: 20px; border-bottom: 2px solid #1B365D;">
                    <h2 style="color: #1B365D; margin: 0;">🏛️ %s</h2>
                    <p style="color: #D97706; font-weight: bold; margin-top: 5px;">Plateforme Numérique Netaa École</p>
                </div>
                <div style="padding: 20px 0;">
                    <p style="font-size: 16px; color: #333333;">Bonjour,</p>
                    <p style="font-size: 15px; color: #555555; line-height: 1.6;">
                        %s
                    </p>
                    <div style="background-color: #f8fafc; border-left: 4px solid #1B365D; padding: 15px; border-radius: 6px; margin: 20px 0;">
                        <p style="margin: 5px 0; color: #1e293b;"><strong>Identifiant :</strong> %s</p>
                        <p style="margin: 5px 0; color: #1e293b;"><strong>Email :</strong> %s</p>
                        <p style="margin: 5px 0; color: #1e293b;"><strong>Mot de passe temporaire :</strong> %s</p>
                    </div>
                    <p style="font-size: 14px; color: #64748b;">
                        Lors de votre première connexion, un <strong>code de sécurité OTP à 6 chiffres</strong> vous sera envoyé par email pour valider votre compte. Vous pourrez ensuite personnaliser votre mot de passe dans vos paramètres.
                    </p>
                    <div style="text-align: center; margin-top: 30px;">
                        <a href="%s" style="background: linear-gradient(135deg, #1B365D, #0f2140); color: #ffffff; padding: 12px 28px; text-decoration: none; font-weight: bold; border-radius: 8px; display: inline-block;">Se connecter à Netaa École</a>
                    </div>
                </div>
                <div style="text-align: center; padding-top: 20px; border-top: 1px solid #e0e0e0; font-size: 12px; color: #94a3b8;">
                    &copy; 2026 Netaa École — République du Mali. Tous droits réservés.
                </div>
            </div>
            """.formatted(headerTitle, contextSentence, user.getUsername(), user.getEmail(), rawPassword != null ? rawPassword : "••••••••", loginUrl);

        sendMailInternal(user.getEmail(), subject, htmlBody);
    }

    @Override
    public void sendOtpEmail(Utilisateur user, String otpCode) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            logger.warn("Aucune adresse email valide pour l'envoi du code OTP utilisateur ID: {}", user.getId());
            return;
        }

        String subject = "🔑 Code de Sécurité OTP Netaa École : " + otpCode;
        String htmlBody = """
            <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 500px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 12px; background-color: #ffffff;">
                <div style="text-align: center; padding-bottom: 15px; border-bottom: 2px solid #1B365D;">
                    <h3 style="color: #1B365D; margin: 0;">🏛️ Netaa École — Sécurité</h3>
                </div>
                <div style="padding: 20px 0; text-align: center;">
                    <p style="font-size: 15px; color: #475569;">Voici votre code de validation pour votre première connexion :</p>
                    <div style="background: #1B365D; color: #ffffff; font-size: 32px; font-weight: 800; letter-spacing: 8px; padding: 16px 24px; border-radius: 10px; display: inline-block; margin: 15px 0;">
                        %s
                    </div>
                    <p style="font-size: 13px; color: #94a3b8; margin-top: 10px;">Ce code est valide pendant <strong>10 minutes</strong>. Ne le partagez avec personne.</p>
                </div>
            </div>
            """.formatted(otpCode);

        logger.info("🔑 [SÉCURITÉ NETAA] CODE OTP GÉNÉRÉ POUR [{}] : {}", user.getEmail(), otpCode);
        sendMailInternal(user.getEmail(), subject, htmlBody);
    }

    @Override
    public void sendSubscriptionWarningEmail(com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement etab, long joursRestants) {
        String recipient = (etab.getEmailContact() != null && !etab.getEmailContact().isBlank()) 
                ? etab.getEmailContact() 
                : "diarrassoubaa505@gmail.com";

        String subject = "⚠️ ALERTE ABONNEMENT : Expiration dans " + joursRestants + " jours (" + etab.getNom() + ")";
        String formattedDate = etab.getDateExpirationAbonnement() != null 
                ? etab.getDateExpirationAbonnement().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
                : "prochainement";

        String htmlBody = """
            <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #f59e0b; border-radius: 12px; background-color: #ffffff;">
                <div style="text-align: center; padding-bottom: 20px; border-bottom: 2px solid #d97706;">
                    <h2 style="color: #d97706; margin: 0;">⚠️ Rappel d'Expiration d'Abonnement</h2>
                    <p style="color: #1B365D; font-weight: bold; margin-top: 5px;">%s</p>
                </div>
                <div style="padding: 20px 0;">
                    <p style="font-size: 16px; color: #333333;">Bonjour Administrateur,</p>
                    <p style="font-size: 15px; color: #555555; line-height: 1.6;">
                        L'abonnement Netaa École de l'établissement <strong>%s</strong> (Plan <strong>%s</strong>) arrive à son terme le <strong>%s</strong> (dans <strong>%d jours</strong>).
                    </p>
                    <div style="background-color: #fffbeb; border-left: 4px solid #f59e0b; padding: 15px; border-radius: 6px; margin: 20px 0;">
                        <p style="margin: 0; color: #b45309; font-size: 14px;">
                            Afin d'éviter toute interruption des services d'accès pour la direction, les enseignants et les parents, veuillez procéder au renouvellement de l'abonnement avant la date d'échéance.
                        </p>
                    </div>
                    <div style="text-align: center; margin-top: 30px;">
                        <a href="%s" style="background: #d97706; color: #ffffff; padding: 12px 28px; text-decoration: none; font-weight: bold; border-radius: 8px; display: inline-block;">Accéder à Netaa École</a>
                    </div>
                </div>
                <div style="text-align: center; padding-top: 20px; border-top: 1px solid #e0e0e0; font-size: 12px; color: #94a3b8;">
                    &copy; 2026 Netaa École — République du Mali. Tous droits réservés.
                </div>
            </div>
            """.formatted(etab.getNom(), etab.getNom(), etab.getPlanTarifaire(), formattedDate, joursRestants, frontendUrl + "/login");

        logger.info("⚠️ [ABONNEMENT] Envoi de l'alerte d'expiration pour l'établissement [{}] à [{}]", etab.getNom(), recipient);
        sendMailInternal(recipient, subject, htmlBody);
        
        if (!"diarrassoubaa505@gmail.com".equalsIgnoreCase(recipient)) {
            sendMailInternal("diarrassoubaa505@gmail.com", "[SUPER-ADMIN] Alerte Expiration Écoles : " + etab.getNom(), htmlBody);
        }
    }

    @Value("${BREVO_API_KEY:${brevo.api-key:}}")
    private String brevoApiKey;

    private void sendMailInternal(String to, String subject, String htmlContent) {
        // Priority 1: Send via Brevo REST API v3 using BREVO_API_KEY
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            try {
                String senderEmail = (fromEmail != null && !fromEmail.isBlank() && fromEmail.contains("@") && !fromEmail.contains("netaa-ecole.ml")) 
                        ? fromEmail 
                        : "diarrassoubaa505@gmail.com";

                String jsonPayload = """
                    {
                      "sender": { "name": "Netaa École", "email": "%s" },
                      "to": [ { "email": "%s" } ],
                      "subject": "%s",
                      "htmlContent": %s
                    }
                    """.formatted(
                        senderEmail,
                        to,
                        escapeJson(subject),
                        escapeJsonString(htmlContent)
                    );

                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("https://api.brevo.com/v3/smtp/email"))
                        .header("api-key", brevoApiKey.trim())
                        .header("Content-Type", "application/json")
                        .header("accept", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload, java.nio.charset.StandardCharsets.UTF_8))
                        .build();

                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    logger.info("📧 Email Brevo REST API envoyé avec succès à [{}] (Code HTTP: {})", to, response.statusCode());
                    return;
                } else {
                    logger.warn("⚠️ Brevo REST API a répondu avec statut {}: {}", response.statusCode(), response.body());
                }
            } catch (Exception e) {
                logger.error("❌ Erreur Brevo REST API : {}", e.getMessage());
            }
        }

        // Priority 2: Fallback to JavaMailSender SMTP
        try {
            if (mailSender != null) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail.isBlank() ? "noreply@netaa-ecole.ml" : fromEmail);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);
                mailSender.send(message);
                logger.info("📧 Email SMTP envoyé avec succès à : {}", to);
            } else {
                logger.info("⚠️ Aucun expéditeur configuré (Mode simulation). Contenu du mail pour [{}]: {}", to, subject);
            }
        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'envoi de l'email SMTP à {}: {}", to, e.getMessage());
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private String escapeJsonString(String text) {
        if (text == null) return "\"\"";
        return "\"" + text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}
