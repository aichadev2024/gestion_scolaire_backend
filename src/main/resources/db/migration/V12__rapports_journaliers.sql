CREATE TABLE rapports_journaliers (
    id BIGSERIAL PRIMARY KEY,
    eleve_id BIGINT NOT NULL REFERENCES eleves(id),
    etablissement_id BIGINT REFERENCES etablissements(id),
    date DATE NOT NULL,
    repas VARCHAR(50),
    sieste_faite BOOLEAN,
    duree_sieste_minutes INTEGER,
    changes_couches INTEGER,
    humeur VARCHAR(50),
    notes TEXT,
    redige_par_id BIGINT REFERENCES utilisateurs(id),
    date_creation TIMESTAMP NOT NULL DEFAULT now(),
    date_modification TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (eleve_id, date)
);

CREATE INDEX idx_rapports_journaliers_eleve ON rapports_journaliers(eleve_id);
CREATE INDEX idx_rapports_journaliers_etablissement ON rapports_journaliers(etablissement_id);
