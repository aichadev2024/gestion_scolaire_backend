package com.gestionscolaire.gestion_scolaire_backend.core.config;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Role;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Vérifie le garde-fou sur le secret JWT et le cycle génération / lecture d'un token. */
class JwtServiceTest {

    private static JwtProperties props(String secret) {
        JwtProperties p = new JwtProperties();
        p.setSecret(secret);
        p.setExpirationMs(3_600_000L);
        return p;
    }

    private static Utilisateur utilisateur() {
        return Utilisateur.builder()
                .id(42L)
                .username("aicha.diarra")
                .role(Role.builder().nom("ADMIN").build())
                .build();
    }

    @Test
    void refuseUnSecretAbsent() {
        assertThrows(IllegalStateException.class, () -> new JwtService(props("   ")));
        assertThrows(IllegalStateException.class, () -> new JwtService(props(null)));
    }

    @Test
    void refuseUnSecretTropCourt() {
        assertThrows(IllegalStateException.class, () -> new JwtService(props("trop-court")));
    }

    @Test
    void refuseLeSecretDExemplePublic() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService(props("gestion-scolaire-jwt-secret-key-2026-changez-en-production")));
    }

    @Test
    void accepteUnSecretValideEtProduitUnTokenLisible() {
        JwtService service = new JwtService(props("un-secret-de-test-suffisamment-long-0123456789"));

        String token = service.generateToken(utilisateur());

        assertNotNull(token);
        assertTrue(service.isTokenValid(token));
        assertEquals("aicha.diarra", service.extractUsername(token));
    }
}
