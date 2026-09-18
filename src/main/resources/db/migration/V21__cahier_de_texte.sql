-- Cahier de texte : une ligne par séance de cours renseignée par l'enseignant (ce qui a été enseigné,
-- devoirs donnés) ou déclarée non effectuée. Sert aussi à mesurer l'effectivité des cours face à
-- l'emploi du temps.
CREATE TABLE seances_cours (
    id BIGSERIAL PRIMARY KEY,
    classe_matiere_id BIGINT NOT NULL REFERENCES classes_matieres(id),
    etablissement_id BIGINT REFERENCES etablissements(id),
    enregistre_par_id BIGINT REFERENCES utilisateurs(id),
    date DATE NOT NULL,
    heure_debut TIME,
    heure_fin TIME,
    effectue BOOLEAN NOT NULL DEFAULT TRUE,
    titre VARCHAR(200),
    contenu TEXT,
    devoirs TEXT,
    motif_non_effectue VARCHAR(255),
    date_creation TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_seances_cours_cm_date ON seances_cours(classe_matiere_id, date);
CREATE INDEX idx_seances_cours_etab_date ON seances_cours(etablissement_id, date);
