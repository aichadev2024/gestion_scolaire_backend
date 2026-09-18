-- Rapport de niveau : un enseignant signale à la direction le niveau d'une classe dans sa matière
-- et les élèves en difficulté, pour corriger les lacunes. La direction le marque comme traité.
CREATE TABLE rapports_niveau (
    id BIGSERIAL PRIMARY KEY,
    classe_matiere_id BIGINT NOT NULL REFERENCES classes_matieres(id),
    etablissement_id BIGINT REFERENCES etablissements(id),
    auteur_id BIGINT REFERENCES utilisateurs(id),
    periode VARCHAR(30) NOT NULL,
    niveau_global VARCHAR(20) NOT NULL,
    commentaire TEXT,
    est_traite BOOLEAN NOT NULL DEFAULT FALSE,
    reponse_direction TEXT,
    date_creation TIMESTAMP NOT NULL DEFAULT now(),
    date_traitement TIMESTAMP
);
CREATE INDEX idx_rapports_niveau_etab ON rapports_niveau(etablissement_id, est_traite);
CREATE INDEX idx_rapports_niveau_cm ON rapports_niveau(classe_matiere_id);

CREATE TABLE rapports_niveau_eleves (
    id BIGSERIAL PRIMARY KEY,
    rapport_id BIGINT NOT NULL REFERENCES rapports_niveau(id) ON DELETE CASCADE,
    eleve_id BIGINT NOT NULL REFERENCES eleves(id),
    moyenne DOUBLE PRECISION,
    commentaire VARCHAR(500)
);
CREATE INDEX idx_rapports_niveau_eleves_rapport ON rapports_niveau_eleves(rapport_id);
