-- Tokens FCM (Firebase Cloud Messaging) pour l'envoi de notifications push. Un utilisateur peut
-- avoir plusieurs tokens (téléphone Android + navigateur web) — pas de collection sur
-- utilisateurs, une entité séparée comme le reste du codebase (pas de relation OneToMany).
CREATE TABLE device_tokens (
    id BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
    token TEXT NOT NULL UNIQUE,
    plateforme VARCHAR(20) NOT NULL,
    date_creation TIMESTAMP NOT NULL DEFAULT now(),
    date_derniere_utilisation TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_device_tokens_utilisateur ON device_tokens(utilisateur_id);
