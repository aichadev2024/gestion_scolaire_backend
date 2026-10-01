-- Disponibilités/indisponibilités déclarées par les enseignants, consultées par la
-- direction pour construire l'emploi du temps (l'enseignant ne crée jamais lui-même
-- de créneau de cours — voir EmploiDuTempsController).
CREATE TABLE disponibilites_enseignant (
    id BIGSERIAL PRIMARY KEY,
    enseignant_id BIGINT NOT NULL REFERENCES enseignants(id) ON DELETE CASCADE,
    etablissement_id BIGINT NOT NULL REFERENCES etablissements(id) ON DELETE CASCADE,
    jour_semaine INTEGER NOT NULL,
    heure_debut TIME NOT NULL,
    heure_fin TIME NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
    commentaire TEXT,
    date_creation TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_disponibilites_enseignant ON disponibilites_enseignant(enseignant_id);
CREATE INDEX idx_disponibilites_etablissement ON disponibilites_enseignant(etablissement_id);
