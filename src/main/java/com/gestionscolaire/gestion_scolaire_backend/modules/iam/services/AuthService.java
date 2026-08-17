package com.gestionscolaire.gestion_scolaire_backend.modules.iam.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto.AuthResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto.LoginRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse verifyOtp(com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto.VerifyOtpRequest request);
    AuthResponse resendOtp(Long utilisateurId);
}


