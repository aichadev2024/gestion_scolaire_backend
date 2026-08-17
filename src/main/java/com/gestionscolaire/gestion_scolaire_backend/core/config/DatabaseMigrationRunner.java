package com.gestionscolaire.gestion_scolaire_backend.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationRunner.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrateDatabaseSchema() {
        logger.info("🔧 Vérification et migration de la base de données PostgreSQL...");

        try {
            // Drop NOT NULL constraint on classe_matiere_id for pause/recreation creneaux
            jdbcTemplate.execute("ALTER TABLE emplois_du_temps ALTER COLUMN classe_matiere_id DROP NOT NULL;");
            logger.info("✅ Contrainte NOT NULL supprimée de emplois_du_temps.classe_matiere_id");
        } catch (Exception e) {
            logger.debug("Remarque lors du DROP NOT NULL (classe_matiere_id) : {}", e.getMessage());
        }

        try {
            // Drop NOT NULL constraint on salle for pause/recreation creneaux
            jdbcTemplate.execute("ALTER TABLE emplois_du_temps ALTER COLUMN salle DROP NOT NULL;");
            logger.info("✅ Contrainte NOT NULL supprimée de emplois_du_temps.salle");
        } catch (Exception e) {
            logger.debug("Remarque lors du DROP NOT NULL (salle) : {}", e.getMessage());
        }

        try {
            // Add classe_id column if missing
            jdbcTemplate.execute("ALTER TABLE emplois_du_temps ADD COLUMN IF NOT EXISTS classe_id BIGINT REFERENCES classes(id) ON DELETE CASCADE;");
            jdbcTemplate.execute("ALTER TABLE emplois_du_temps ADD COLUMN IF NOT EXISTS type_creneau VARCHAR(30) DEFAULT 'COURS';");
            jdbcTemplate.execute("ALTER TABLE emplois_du_temps ADD COLUMN IF NOT EXISTS libelle_pause VARCHAR(100);");
            logger.info("✅ Colonnes classe_id, type_creneau et libelle_pause vérifiées dans emplois_du_temps");
        } catch (Exception e) {
            logger.debug("Remarque lors de l'ajout des colonnes : {}", e.getMessage());
        }
    }
}
