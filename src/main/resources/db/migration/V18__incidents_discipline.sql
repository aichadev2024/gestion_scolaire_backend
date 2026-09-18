-- Fiche de suivi de discipline (retard, absence, tenue non portée, refus d'exercice) — remplie
-- par le surveillant général ou la direction, notifie automatiquement le(s) parent(s) via le
-- mécanisme de notification existant.
CREATE TABLE incidents_discipline (
    id BIGSERIAL PRIMARY KEY,
    eleve_id BIGINT NOT NULL REFERENCES eleves(id),
    classe_id BIGINT NOT NULL REFERENCES classes(id),
    classe_matiere_id BIGINT REFERENCES classes_matieres(id),
    etablissement_id BIGINT REFERENCES etablissements(id),
    enregistre_par_id BIGINT REFERENCES utilisateurs(id),
    date DATE NOT NULL,
    heure TIME NOT NULL,
    statut VARCHAR(30) NOT NULL,
    commentaire TEXT,
    est_traite BOOLEAN NOT NULL DEFAULT FALSE,
    notes_traitement TEXT,
    date_creation TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_incidents_discipline_eleve ON incidents_discipline(eleve_id);
CREATE INDEX idx_incidents_discipline_classe_date ON incidents_discipline(classe_id, date);
