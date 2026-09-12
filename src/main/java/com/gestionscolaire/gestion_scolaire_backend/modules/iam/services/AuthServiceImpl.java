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
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EleveRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    /** Durée de validité d'un code OTP (doit rester cohérent avec le texte de l'e-mail). */
    private static final int OTP_VALIDITE_MINUTES = 10;
    /** Nombre maximum de tentatives OTP échouées avant verrouillage du code. */
    private static final int OTP_MAX_TENTATIVES = 5;
    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    /** Génère un code OTP à 6 chiffres cryptographiquement aléatoire (000000–999999). */
    private static String genererCodeOtp() {
        return String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
    }

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UtilisateurRepository utilisateurRepository;
    private final ProfilRepository profilRepository;
    private final EmailService emailService;
    private final EleveRepository eleveRepository;
    private final com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services.TarifPlanService tarifPlanService;

    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UtilisateurRepository utilisateurRepository,
            ProfilRepository profilRepository,
            EmailService emailService,
            EleveRepository eleveRepository,
            com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services.TarifPlanService tarifPlanService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.utilisateurRepository = utilisateurRepository;
        this.profilRepository = profilRepository;
        this.emailService = emailService;
        this.eleveRepository = eleveRepository;
        this.tarifPlanService = tarifPlanService;
    }

    private Integer limiteEnseignantsPour(Utilisateur utilisateur) {
        if (utilisateur.getEtablissement() == null) return null;
        return tarifPlanService.obtenirLimiteEnseignants(utilisateur.getEtablissement().getPlanTarifaire());
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

        // Première connexion : Envoi d'un OTP par Email uniquement si l'utilisateur possède une adresse e-mail
        if (Boolean.TRUE.equals(utilisateur.getEstPremierLogin())) {
            if (utilisateur.getEmail() != null && !utilisateur.getEmail().isBlank()) {
                String otpCode = genererCodeOtp();
                utilisateur.setOtpCode(otpCode);
                utilisateur.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_VALIDITE_MINUTES));
                utilisateur.setOtpTentatives(0);
                utilisateurRepository.save(utilisateur);

                emailService.sendOtpEmail(utilisateur, otpCode);

                return AuthResponse.builder()
                        .requiresOtp(true)
                        .utilisateurId(utilisateur.getId())
                        .email(utilisateur.getEmail())
                        .username(utilisateur.getUsername())
                        .role(utilisateur.getRole().getNom())
                        .message("Premier login détecté. Un code OTP de confirmation a été envoyé à " + utilisateur.getEmail() + ". Veuillez consulter vos mails pour le valider.")
                        .build();
            } else {
                // Utilisateur sans e-mail (ex: Enseignant / Parent sans mail) : valider automatiquement le premier login
                utilisateur.setEstPremierLogin(false);
                utilisateurRepository.save(utilisateur);
            }
        }

        Profil profil = profilRepository.findByUtilisateurId(utilisateur.getId()).orElse(null);
        String token = jwtService.generateToken(utilisateur);

        Long eleveId = null;
        String classeNom = null;
        if (utilisateur.getRole() != null && "ELEVE".equalsIgnoreCase(utilisateur.getRole().getNom())) {
            Eleve eleve = eleveRepository.findByProfilUtilisateurId(utilisateur.getId()).orElse(null);
            if (eleve != null) {
                eleveId = eleve.getId();
                if (eleve.getClasse() != null) {
                    classeNom = eleve.getClasse().getNom();
                }
            }
        }

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
                .etablissementLogoUrl(utilisateur.getEtablissement() != null ? utilisateur.getEtablissement().getLogoUrl() : null)
                .etablissementDevise(utilisateur.getEtablissement() != null ? utilisateur.getEtablissement().getDevise() : "FCFA")
                .etablissementSlogan(utilisateur.getEtablissement() != null ? utilisateur.getEtablissement().getSlogan() : null)
                .etablissementPlanTarifaire(utilisateur.getEtablissement() != null ? utilisateur.getEtablissement().getPlanTarifaire() : null)
                .etablissementMaxEnseignants(limiteEnseignantsPour(utilisateur))
                .eleveId(eleveId)
                .classeNom(classeNom)
                .build();
    }

    @Override
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        if (request.getUtilisateurId() == null || request.getOtpCode() == null || request.getOtpCode().isBlank()) {
            throw new BadRequestException("ID utilisateur et code OTP requis.");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(request.getUtilisateurId())
                .orElseThrow(() -> new BadRequestException("Utilisateur introuvable."));

        if (utilisateur.getOtpCode() == null || utilisateur.getOtpExpiry() == null) {
            throw new BadRequestException("Aucun code OTP en attente. Veuillez vous reconnecter pour en recevoir un nouveau.");
        }

        if (LocalDateTime.now().isAfter(utilisateur.getOtpExpiry())) {
            utilisateur.setOtpCode(null);
            utilisateur.setOtpExpiry(null);
            utilisateur.setOtpTentatives(0);
            utilisateurRepository.save(utilisateur);
            throw new BadRequestException("Le code OTP a expiré. Veuillez cliquer sur « Renvoyer le code » pour en recevoir un nouveau par email.");
        }

        int tentatives = utilisateur.getOtpTentatives() == null ? 0 : utilisateur.getOtpTentatives();
        if (tentatives >= OTP_MAX_TENTATIVES) {
            utilisateur.setOtpCode(null);
            utilisateur.setOtpExpiry(null);
            utilisateur.setOtpTentatives(0);
            utilisateurRepository.save(utilisateur);
            throw new BadRequestException("Trop de tentatives échouées. Ce code a été invalidé — cliquez sur « Renvoyer le code ».");
        }

        if (!utilisateur.getOtpCode().equals(request.getOtpCode().trim())) {
            int restant = OTP_MAX_TENTATIVES - (tentatives + 1);
            utilisateur.setOtpTentatives(tentatives + 1);
            utilisateurRepository.save(utilisateur);
            if (restant <= 0) {
                throw new BadRequestException("Code OTP incorrect. Ce code a été invalidé après trop de tentatives — cliquez sur « Renvoyer le code ».");
            }
            throw new BadRequestException("Code OTP incorrect. Il vous reste " + restant + " tentative(s).");
        }

        // Validation réussie
        utilisateur.setEstPremierLogin(false);
        utilisateur.setOtpCode(null);
        utilisateur.setOtpExpiry(null);
        utilisateur.setOtpTentatives(0);
        utilisateurRepository.save(utilisateur);

        Profil profil = profilRepository.findByUtilisateurId(utilisateur.getId()).orElse(null);
        String token = jwtService.generateToken(utilisateur);

        Long eleveId = null;
        String classeNom = null;
        if (utilisateur.getRole() != null && "ELEVE".equalsIgnoreCase(utilisateur.getRole().getNom())) {
            Eleve eleve = eleveRepository.findByProfilUtilisateurId(utilisateur.getId()).orElse(null);
            if (eleve != null) {
                eleveId = eleve.getId();
                if (eleve.getClasse() != null) {
                    classeNom = eleve.getClasse().getNom();
                }
            }
        }

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
                .etablissementLogoUrl(utilisateur.getEtablissement() != null ? utilisateur.getEtablissement().getLogoUrl() : null)
                .etablissementDevise(utilisateur.getEtablissement() != null ? utilisateur.getEtablissement().getDevise() : "FCFA")
                .etablissementSlogan(utilisateur.getEtablissement() != null ? utilisateur.getEtablissement().getSlogan() : null)
                .etablissementPlanTarifaire(utilisateur.getEtablissement() != null ? utilisateur.getEtablissement().getPlanTarifaire() : null)
                .etablissementMaxEnseignants(limiteEnseignantsPour(utilisateur))
                .eleveId(eleveId)
                .classeNom(classeNom)
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

        String otpCode = genererCodeOtp();
        utilisateur.setOtpCode(otpCode);
        utilisateur.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_VALIDITE_MINUTES));
        utilisateur.setOtpTentatives(0);
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


