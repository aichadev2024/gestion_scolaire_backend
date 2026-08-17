package com.gestionscolaire.gestion_scolaire_backend.modules.iam.services;

import com.gestionscolaire.gestion_scolaire_backend.core.config.JwtService;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto.AuthResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto.LoginRequest;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.ProfilRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.services.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.gestionscolaire.gestion_scolaire_backend.core.services.EmailService;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto.VerifyOtpRequest;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UtilisateurRepository utilisateurRepository;
    private final ProfilRepository profilRepository;
    private final EmailService emailService;

    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UtilisateurRepository utilisateurRepository,
            ProfilRepository profilRepository,
            EmailService emailService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.utilisateurRepository = utilisateurRepository;
        this.profilRepository = profilRepository;
        this.emailService = emailService;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getIdentifiant(), request.getMotDePasse()));
        } catch (AuthenticationException e) {
            throw new BadRequestException("Identifiants invalides");
        }

        Utilisateur utilisateur = utilisateurRepository.findByUsernameOrEmail(request.getIdentifiant(), request.getIdentifiant())
                .orElseThrow(() -> new BadRequestException("Utilisateur introuvable"));

        if (!Boolean.TRUE.equals(utilisateur.getEstActif())) {
            throw new BadRequestException("Compte désactivé");
        }

        if (utilisateur.getEtablissement() != null) {
            com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement etab = utilisateur.getEtablissement();
            if (etab.getStatut() != com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.StatutEtablissement.ACTIF) {
                throw new BadRequestException("L'accès de votre établissement est temporairement suspendu ou clôturé.");
            }
            if (etab.getDateExpirationAbonnement() != null) {
                if (etab.getDateExpirationAbonnement().isBefore(LocalDateTime.now())) {
                    throw new BadRequestException("L'abonnement de votre établissement a expiré le " + 
                            etab.getDateExpirationAbonnement().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + 
                            ". Veuillez contacter l'administration Netaa École pour renouveler votre licence.");
                }
                long daysUntilExpiry = java.time.Duration.between(LocalDateTime.now(), etab.getDateExpirationAbonnement()).toDays();
                if (daysUntilExpiry <= 15) {
                    try {
                        emailService.sendSubscriptionWarningEmail(etab, Math.max(1, daysUntilExpiry));
                    } catch (Exception ignored) {}
                }
            }
        }

        // Première connexion : Envoi obligatoire d'un OTP par Email
        if (Boolean.TRUE.equals(utilisateur.getEstPremierLogin())) {
            String otpCode = String.format("%06d", new Random().nextInt(900000) + 100000);
            utilisateur.setOtpCode(otpCode);
            utilisateur.setOtpExpiry(LocalDateTime.now().plusMinutes(15));
            utilisateurRepository.save(utilisateur);

            emailService.sendOtpEmail(utilisateur, otpCode);

            String emailDest = (utilisateur.getEmail() != null && !utilisateur.getEmail().isBlank()) 
                    ? utilisateur.getEmail() 
                    : "votre adresse mail";

            return AuthResponse.builder()
                    .requiresOtp(true)
                    .utilisateurId(utilisateur.getId())
                    .email(utilisateur.getEmail())
                    .username(utilisateur.getUsername())
                    .role(utilisateur.getRole().getNom())
                    .message("Premier login détecté. Un code OTP de confirmation a été envoyé à " + emailDest + ". Veuillez consulter vos mails pour le valider.")
                    .build();
        }

        Profil profil = profilRepository.findByUtilisateurId(utilisateur.getId()).orElse(null);
        String token = jwtService.generateToken(utilisateur);

        return AuthResponse.builder()
                .requiresOtp(false)
                .token(token)
                .type("Bearer")
                .utilisateurId(utilisateur.getId())
                .email(utilisateur.getEmail())
                .username(utilisateur.getUsername())
                .role(utilisateur.getRole().getNom())
                .prenom(profil != null ? profil.getPrenom() : null)
                .nom(profil != null ? profil.getNom() : null)
                .etablissementId(utilisateur.getEtablissement() != null ? utilisateur.getEtablissement().getId() : null)
                .etablissementNom(utilisateur.getEtablissement() != null ? utilisateur.getEtablissement().getNom() : "Établissement Scolaire")
                .build();
    }

    @Override
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        if (request.getUtilisateurId() == null || request.getOtpCode() == null || request.getOtpCode().isBlank()) {
            throw new BadRequestException("ID utilisateur et code OTP requis.");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(request.getUtilisateurId())
                .orElseThrow(() -> new BadRequestException("Utilisateur introuvable."));

        if (utilisateur.getOtpCode() == null || !utilisateur.getOtpCode().equals(request.getOtpCode().trim())) {
            throw new BadRequestException("Le code OTP saisi est incorrect. Veuillez vérifier le code reçu par email.");
        }

        if (utilisateur.getOtpExpiry() == null || LocalDateTime.now().isAfter(utilisateur.getOtpExpiry())) {
            throw new BadRequestException("Le code OTP a expiré. Veuillez cliquer sur 'Renvoyer le code' pour en recevoir un nouveau par email.");
        }

        // Validation réussie
        utilisateur.setEstPremierLogin(false);
        utilisateur.setOtpCode(null);
        utilisateur.setOtpExpiry(null);
        utilisateurRepository.save(utilisateur);

        Profil profil = profilRepository.findByUtilisateurId(utilisateur.getId()).orElse(null);
        String token = jwtService.generateToken(utilisateur);

        return AuthResponse.builder()
                .requiresOtp(false)
                .token(token)
                .type("Bearer")
                .utilisateurId(utilisateur.getId())
                .email(utilisateur.getEmail())
                .username(utilisateur.getUsername())
                .role(utilisateur.getRole().getNom())
                .prenom(profil != null ? profil.getPrenom() : null)
                .nom(profil != null ? profil.getNom() : null)
                .etablissementId(utilisateur.getEtablissement() != null ? utilisateur.getEtablissement().getId() : null)
                .etablissementNom(utilisateur.getEtablissement() != null ? utilisateur.getEtablissement().getNom() : "Établissement Scolaire")
                .message("Première connexion validée avec succès !")
                .build();
    }

    @Override
    public AuthResponse resendOtp(Long utilisateurId) {
        if (utilisateurId == null) {
            throw new BadRequestException("ID utilisateur requis.");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new BadRequestException("Utilisateur introuvable."));

        String otpCode = String.format("%06d", new Random().nextInt(900000) + 100000);
        utilisateur.setOtpCode(otpCode);
        utilisateur.setOtpExpiry(LocalDateTime.now().plusMinutes(15));
        utilisateurRepository.save(utilisateur);

        emailService.sendOtpEmail(utilisateur, otpCode);

        String emailDest = (utilisateur.getEmail() != null && !utilisateur.getEmail().isBlank()) 
                ? utilisateur.getEmail() 
                : "votre adresse mail";

        return AuthResponse.builder()
                .requiresOtp(true)
                .utilisateurId(utilisateur.getId())
                .email(utilisateur.getEmail())
                .username(utilisateur.getUsername())
                .role(utilisateur.getRole().getNom())
                .message("Un nouveau code OTP a été renvoyé à " + emailDest + ". Veuillez vérifier vos emails.")
                .build();
    }
}


