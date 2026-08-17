package com.gestionscolaire.gestion_scolaire_backend.modules.iam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {
    private Long utilisateurId;
    private String otpCode;
}
