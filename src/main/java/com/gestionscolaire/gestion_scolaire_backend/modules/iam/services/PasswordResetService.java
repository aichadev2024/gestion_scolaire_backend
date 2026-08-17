package com.gestionscolaire.gestion_scolaire_backend.modules.iam.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.PasswordResetToken;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.PasswordResetTokenRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class PasswordResetService {

    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // JavaMailSender est optionnel — si non configuré, on ne plante pas
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.password-reset-token-expiry-minutes:30}")
    private int expiryMinutes;

    /**
     * Génère un token et envoie l'email de réinitialisation.
     * Retourne le token dans tous les cas (utile pour dev/test si mail non configuré).
     */
    public String demanderReinitialisation(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun compte associé à cet email."));

        // Supprimer les anciens tokens
        tokenRepository.deleteByUtilisateurId(utilisateur.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .utilisateur(utilisateur)
                .expiryDate(LocalDateTime.now().plusMinutes(expiryMinutes))
                .estUtilise(false)
                .build();
        tokenRepository.save(resetToken);

        String resetLink = frontendUrl + "/reset-password?token=" + token;

        // Envoi email si le serveur mail est configuré
        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email);
                message.setSubject("Réinitialisation de votre mot de passe — Netaa");
                message.setText(
                    "Bonjour,\n\n" +
                    "Vous avez demandé à réinitialiser votre mot de passe sur Netaa.\n\n" +
                    "Cliquez sur ce lien (valable " + expiryMinutes + " minutes) :\n" + resetLink + "\n\n" +
                    "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.\n\n" +
                    "L'équipe Netaa"
                );
                mailSender.send(message);
            } catch (Exception e) {
                // Ne pas bloquer si l'email échoue
                System.err.println("[PasswordReset] Erreur envoi email : " + e.getMessage());
            }
        }

        return token; // Retourné pour affichage en dev
    }

    /**
     * Valide le token et applique le nouveau mot de passe.
     */
    public void reinitialiserMotDePasse(String token, String nouveauMotDePasse) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Token invalide ou introuvable."));

        if (resetToken.isExpired()) {
            throw new BadRequestException("Ce lien a expiré. Veuillez faire une nouvelle demande.");
        }
        if (Boolean.TRUE.equals(resetToken.getEstUtilise())) {
            throw new BadRequestException("Ce lien a déjà été utilisé.");
        }

        Utilisateur utilisateur = resetToken.getUtilisateur();
        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        utilisateurRepository.save(utilisateur);

        resetToken.setEstUtilise(true);
        tokenRepository.save(resetToken);
    }

    /**
     * Change le mot de passe pour un utilisateur connecté.
     */
    public void changerMotDePasse(Long utilisateurId, String ancienMdp, String nouveauMdp) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));

        if (!passwordEncoder.matches(ancienMdp, utilisateur.getMotDePasse())) {
            throw new BadRequestException("L'ancien mot de passe est incorrect.");
        }

        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMdp));
        utilisateurRepository.save(utilisateur);
    }
}


