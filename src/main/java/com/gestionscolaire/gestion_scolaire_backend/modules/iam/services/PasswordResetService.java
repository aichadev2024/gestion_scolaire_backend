package com.gestionscolaire.gestion_scolaire_backend.modules.iam.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.PasswordResetToken;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.PasswordResetTokenRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    @Autowired private com.gestionscolaire.gestion_scolaire_backend.core.services.EmailService emailService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.password-reset-token-expiry-minutes:30}")
    private int expiryMinutes;

    /**
     * Génère un token et l'envoie PAR E-MAIL uniquement. Ne renvoie jamais le token à l'appelant et ne
     * révèle pas si l'adresse correspond à un compte (réponse identique dans tous les cas) : sinon
     * n'importe qui pouvait réinitialiser le mot de passe de n'importe quel compte, ou deviner
     * quelles adresses sont inscrites.
     */
    public void demanderReinitialisation(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email).orElse(null);
        if (utilisateur == null) {
            return;
        }

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

        try {
            emailService.sendPasswordResetEmail(email, frontendUrl + "/reset-password?token=" + token, expiryMinutes);
        } catch (Exception e) {
            // Ne pas bloquer ni révéler l'échec à l'appelant
            System.err.println("[PasswordReset] Erreur envoi email : " + e.getMessage());
        }
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


