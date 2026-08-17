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

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
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


