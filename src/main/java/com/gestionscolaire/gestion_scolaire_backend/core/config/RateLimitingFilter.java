package com.gestionscolaire.gestion_scolaire_backend.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitation de débit basique (fenêtre glissante, en mémoire) sur les points d'entrée
 * d'authentification sensibles, pour freiner le bourrage d'identifiants et le brute-force OTP.
 *
 * <p>Stockage en mémoire du processus : suffisant pour un déploiement mono-instance.
 * Pour une exécution multi-instances, remplacer par un compteur partagé (Redis / Bucket4j).</p>
 *
 * <p>Volontairement instancié dans {@code SecurityConfig} (et non annoté {@code @Component})
 * afin d'éviter la double inscription dans la chaîne de filtres du conteneur.</p>
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    /** Chemins protégés (comparaison exacte). */
    private static final Set<String> CHEMINS_PROTEGES = Set.of(
            "/api/auth/login",
            "/api/auth/verify-otp",
            "/api/auth/resend-otp",
            "/api/auth/forgot-password"
    );

    private static final long FENETRE_MS = 60_000L;
    private static final int MAX_REQUETES_PAR_FENETRE = 10;
    /** Garde-fou mémoire : au-delà, on purge tout le cache. */
    private static final int TAILLE_MAX_CACHE = 50_000;

    private final ConcurrentHashMap<String, Deque<Long>> historique = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && CHEMINS_PROTEGES.contains(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String cle = clientIp(request) + "|" + request.getRequestURI();
        long maintenant = System.currentTimeMillis();

        if (historique.size() > TAILLE_MAX_CACHE) {
            historique.clear();
        }

        boolean autorise = enregistrerEtVerifier(cle, maintenant);
        if (autorise) {
            filterChain.doFilter(request, response);
            return;
        }

        logger.warn("429 Rate limit dépassé pour {} sur {}", clientIp(request), request.getRequestURI());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(FENETRE_MS / 1000));
        String body = """
                {"timestamp":"%s","status":429,"error":"Too Many Requests",\
                "message":"Trop de tentatives. Veuillez réessayer dans une minute.","path":"%s"}\
                """.formatted(LocalDateTime.now(), request.getRequestURI());
        response.getWriter().write(body);
    }

    private boolean enregistrerEtVerifier(String cle, long maintenant) {
        Deque<Long> fenetre = historique.computeIfAbsent(cle, k -> new ArrayDeque<>());
        synchronized (fenetre) {
            long limite = maintenant - FENETRE_MS;
            while (!fenetre.isEmpty() && fenetre.peekFirst() < limite) {
                fenetre.pollFirst();
            }
            if (fenetre.size() >= MAX_REQUETES_PAR_FENETRE) {
                return false;
            }
            fenetre.addLast(maintenant);
            return true;
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int virgule = forwarded.indexOf(',');
            return (virgule > 0 ? forwarded.substring(0, virgule) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
