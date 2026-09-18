-- Décision de fin d'année (passage / redoublement) prise par la direction pour un élève, à partir de la
-- proposition calculée sur ses moyennes. Une décision par élève et par année scolaire.
CREATE TABLE decisions_passage (
    id BIGSERIAL PRIMARY KEY,
    eleve_id BIGINT NOT NULL REFERENCES eleves(id),
    etablissement_id BIGINT REFERENCES etablissements(id),
    decide_par_id BIGINT REFERENCES utilisateurs(id),
    annee_scolaire VARCHAR(20) NOT NULL,
    decision VARCHAR(20) NOT NULL,
    commentaire VARCHAR(500),
    date_decision TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_decision_eleve_annee UNIQUE (eleve_id, annee_scolaire)
);
CREATE INDEX idx_decisions_passage_etab ON decisions_passage(etablissement_id, annee_scolaire);
