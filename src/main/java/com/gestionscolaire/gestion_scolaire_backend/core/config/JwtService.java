package com.gestionscolaire.gestion_scolaire_backend.core.config;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    /** Valeur d'exemple publiée dans le dépôt : interdite en exécution. */
    private static final String SECRET_EXEMPLE_PUBLIC =
            "gestion-scolaire-jwt-secret-key-2026-changez-en-production";
    private static final int LONGUEUR_MIN_SECRET = 32;

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        String secret = validerSecret(jwtProperties.getSecret());
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Valide le secret JWT au démarrage. Échoue vite (contexte Spring non démarré)
     * plutôt que de servir des tokens signés avec une clé faible ou publique.
     */
    private static String validerSecret(String secret) {
        String valeur = secret == null ? "" : secret.strip();
        if (valeur.isEmpty()) {
            throw new IllegalStateException(
                    "JWT_SECRET est obligatoire mais absent. Définissez la variable d'environnement "
                    + "JWT_SECRET (>= " + LONGUEUR_MIN_SECRET + " caractères). Générer : openssl rand -base64 48");
        }
        if (valeur.length() < LONGUEUR_MIN_SECRET) {
            throw new IllegalStateException(
                    "JWT_SECRET est trop court (" + valeur.length() + " caractères). Minimum "
                    + LONGUEUR_MIN_SECRET + " caractères pour HMAC-SHA256.");
        }
        if (SECRET_EXEMPLE_PUBLIC.equals(valeur)) {
            throw new IllegalStateException(
                    "JWT_SECRET utilise la valeur d'exemple publique du dépôt. Générez un secret unique "
                    + "et définissez-le via la variable d'environnement JWT_SECRET.");
        }
        return valeur;
    }

    public String generateToken(Utilisateur utilisateur) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpirationMs());

        return Jwts.builder()
                .subject(utilisateur.getUsername())
                .claim("userId", utilisateur.getId())
                .claim("role", utilisateur.getRole().getNom())
                .claim("etablissementId", utilisateur.getEtablissement() != null ? utilisateur.getEtablissement().getId() : null)
                .claim("etablissementCode", utilisateur.getEtablissement() != null ? utilisateur.getEtablissement().getCode() : null)
                .claim("etablissementNom", utilisateur.getEtablissement() != null ? utilisateur.getEtablissement().getNom() : "Netaa École")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}


