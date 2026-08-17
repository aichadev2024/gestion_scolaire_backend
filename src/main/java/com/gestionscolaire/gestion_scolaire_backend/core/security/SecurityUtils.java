package com.gestionscolaire.gestion_scolaire_backend.core.security;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BadRequestException("Utilisateur non authentifié");
        }
        return userDetails;
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getUtilisateur().getId();
    }
}


