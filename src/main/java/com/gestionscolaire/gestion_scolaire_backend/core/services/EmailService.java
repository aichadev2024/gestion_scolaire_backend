package com.gestionscolaire.gestion_scolaire_backend.core.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;

public interface EmailService {
    void sendWelcomeEmail(Utilisateur user, String rawPassword);
    void sendOtpEmail(Utilisateur user, String otpCode);
    void sendSubscriptionWarningEmail(com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement etab, long joursRestants);
    void sendEtablissementCreatedWithPdf(com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement etab, String adminEmail, String adminPassword, byte[] pdfBytes);
}
