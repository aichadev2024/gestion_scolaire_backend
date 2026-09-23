-- Sujets de devoir et d'examen qu'un enseignant transmet à la direction avant de les donner
-- aux élèves (contrôle avant diffusion). Le fichier lui-même est sur R2, seule l'URL est ici
-- (même schéma que documents_eleve).
CREATE TABLE sujets_devoirs (
    id BIGSERIAL PRIMARY KEY,
    classe_matiere_id BIGINT NOT NULL REFERENCES classes_matieres(id),
    enseignant_id BIGINT NOT NULL REFERENCES enseignants(id),
    etablissement_id BIGINT REFERENCES etablissements(id),
    type VARCHAR(20) NOT NULL,
    titre VARCHAR(200) NOT NULL,
    description TEXT,
    url TEXT NOT NULL,
    content_type VARCHAR(100),
    taille_octets BIGINT,
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    commentaire_direction TEXT,
    traite_par_id BIGINT REFERENCES utilisateurs(id),
    date_envoi TIMESTAMP NOT NULL DEFAULT now(),
    date_traitement TIMESTAMP
);
CREATE INDEX idx_sujets_devoirs_etab ON sujets_devoirs(etablissement_id, statut);
CREATE INDEX idx_sujets_devoirs_enseignant ON sujets_devoirs(enseignant_id);
CREATE INDEX idx_sujets_devoirs_cm ON sujets_devoirs(classe_matiere_id);
