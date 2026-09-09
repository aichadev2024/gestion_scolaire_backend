package com.gestionscolaire.gestion_scolaire_backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base des tests d'intégration : contexte Spring complet sur une vraie base
 * PostgreSQL éphémère (Testcontainers). Le conteneur est partagé entre les
 * classes de test grâce au cache de contexte Spring. Nécessite Docker.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
}
