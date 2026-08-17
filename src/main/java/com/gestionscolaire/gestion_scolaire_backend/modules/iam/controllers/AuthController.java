package com.gestionscolaire.gestion_scolaire_backend.modules.iam.controllers;

import com.gestionscolaire.gestion_scolaire_backend.core.dto.DtoMapper;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.ProfilRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository;
import com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.services.PasswordResetService;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.services.AuthService;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.services.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UtilisateurService utilisateurService;
    private final DtoMapper dtoMapper;
    private final ProfilRepository profilRepository;
    private final PasswordResetService passwordResetService;
    private final UtilisateurRepository utilisateurRepository;

    public AuthController(
            AuthService authService,
            UtilisateurService utilisateurService,
            DtoMapper dtoMapper,
            ProfilRepository profilRepository,
            PasswordResetService passwordResetService,
            UtilisateurRepository utilisateurRepository
    ) {
        this.authService = authService;
        this.utilisateurService = utilisateurService;
        this.dtoMapper = dtoMapper;
        this.profilRepository = profilRepository;
        this.passwordResetService = passwordResetService;
        this.utilisateurRepository = utilisateurRepository;
    }

    // ── Login ──────────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ── Validation OTP Première Connexion ─────────────────────────────────────
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    // ── Renvoyer Code OTP par Email ───────────────────────────────────────────
    @PostMapping("/resend-otp")
    public ResponseEntity<AuthResponse> resendOtp(@RequestBody Map<String, Long> payload) {
        Long utilisateurId = payload.get("utilisateurId");
        return ResponseEntity.ok(authService.resendOtp(utilisateurId));
    }

    // ── Premier admin (appelé une seule fois à l'installation) ────────────────
    @PostMapping("/register-first-admin")
    public ResponseEntity<UtilisateurResponse> registerFirstAdmin(@Valid @RequestBody RegisterUtilisateurRequest request) {
        Utilisateur utilisateur = Utilisateur.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .motDePasse(request.getMotDePasse())
                .build();
        Profil profil = dtoMapper.toProfil(request.getProfil());
        Utilisateur saved = utilisateurService.inscrirePremierAdmin(utilisateur, profil);
        Profil savedProfil = profilRepository.findByUtilisateurId(saved.getId()).orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toUtilisateurResponse(saved, savedProfil));
    }

    // ── Inscription Super Admin (Éditeur SaaS) ─────────────────────────────────
    @PostMapping("/register-super-admin")
    public ResponseEntity<UtilisateurResponse> registerSuperAdmin(@Valid @RequestBody RegisterUtilisateurRequest request) {
        Utilisateur utilisateur = Utilisateur.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .motDePasse(request.getMotDePasse())
                .build();
        Profil profil = dtoMapper.toProfil(request.getProfil());
        Utilisateur saved = utilisateurService.inscrireSuperAdmin(utilisateur, profil);
        Profil savedProfil = profilRepository.findByUtilisateurId(saved.getId()).orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toUtilisateurResponse(saved, savedProfil));
    }

    // ── Vérifier si le setup est nécessaire (aucun admin en base) ────────────
    @GetMapping("/check-setup")
    public ResponseEntity<Map<String, Boolean>> checkSetup() {
        boolean adminExists = utilisateurRepository.existsByRoleNom("ADMIN") || utilisateurRepository.existsByRoleNom("SUPER_ADMIN");
        return ResponseEntity.ok(Map.of("setupRequired", !adminExists));
    }

    // ── Vérifier si un Super Admin existe déjà ────────────────────────────────
    @GetMapping("/check-super-admin")
    public ResponseEntity<Map<String, Boolean>> checkSuperAdmin() {
        boolean superAdminExists = utilisateurRepository.existsByRoleNom("SUPER_ADMIN");
        return ResponseEntity.ok(Map.of("exists", superAdminExists));
    }

    // ── Mot de passe oublié : demande de réinitialisation ─────────────────────
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String token = passwordResetService.demanderReinitialisation(request.getEmail());
        // En production, seul l'email est envoyé et on ne retourne pas le token
        // En dev, on retourne le token pour faciliter les tests
        return ResponseEntity.ok(Map.of(
            "message", "Si cet email est enregistré, un lien de réinitialisation a été envoyé.",
            "dev_token", token  // Retirer en production
        ));
    }

    // ── Réinitialisation du mot de passe avec le token ─────────────────────────
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reinitialiserMotDePasse(request.getToken(), request.getNouveauMotDePasse());
        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès. Vous pouvez maintenant vous connecter."));
    }

    // ── Changer mot de passe (utilisateur connecté) ────────────────────────────
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        passwordResetService.changerMotDePasse(userId, request.getAncienMotDePasse(), request.getNouveauMotDePasse());
        return ResponseEntity.ok(Map.of("message", "Mot de passe changé avec succès."));
    }
}



