package com.gestionscolaire.gestion_scolaire_backend.core.tenancy;

import com.gestionscolaire.gestion_scolaire_backend.core.security.CustomUserDetails;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Après authentification JWT, place l'établissement courant dans {@link TenantContext}
 * pour toute la durée de la requête, puis le nettoie.
 *
 * <p>Instancié dans {@code SecurityConfig} (pas de {@code @Component}) pour éviter la
 * double inscription dans la chaîne de filtres du conteneur.</p>
 */
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
                Utilisateur u = details.getUtilisateur();
                String role = (u.getRole() != null) ? u.getRole().getNom() : null;
                if (ROLE_SUPER_ADMIN.equalsIgnoreCase(role)) {
                    TenantContext.set(null, true);
                } else {
                    Long etablissementId = (u.getEtablissement() != null) ? u.getEtablissement().getId() : null;
                    TenantContext.set(etablissementId, false);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
