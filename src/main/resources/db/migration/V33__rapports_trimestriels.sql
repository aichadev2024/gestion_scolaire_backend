-- Bilan (commentaire) et dates retenues pour les rapports de période. En production Hibernate
-- (ddl-auto=update) crée la table tout seul.
CREATE TABLE rapports_trimestriels (
    id BIGSERIAL PRIMARY KEY,
    etablissement_id BIGINT REFERENCES etablissements(id) ON DELETE CASCADE,
    classe_id BIGINT REFERENCES classes(id) ON DELETE CASCADE,
    periode VARCHAR(30) NOT NULL,
    annee_scolaire VARCHAR(20) NOT NULL,
    date_debut DATE,
    date_fin DATE,
    commentaire TEXT,
    modifie_par_id BIGINT REFERENCES utilisateurs(id) ON DELETE SET NULL,
    date_modification TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX uq_rapport_classe ON rapports_trimestriels (etablissement_id, classe_id, periode, annee_scolaire) WHERE classe_id IS NOT NULL;
CREATE UNIQUE INDEX uq_rapport_etablissement ON rapports_trimestriels (etablissement_id, periode, annee_scolaire) WHERE classe_id IS NULL;
