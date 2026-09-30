-- Catalogue des salles de classe — pour les établissements qui nomment leurs salles
-- (« Salle 104 », « Labo de Physique »...) plutôt que de les saisir en texte libre à
-- chaque créneau d'emploi du temps. Purement optionnel : un établissement qui ne crée
-- aucune salle continue à saisir le champ "salle" de emplois_du_temps en texte libre.
CREATE TABLE salles (
    id BIGSERIAL PRIMARY KEY,
    etablissement_id BIGINT REFERENCES etablissements(id),
    nom VARCHAR(100) NOT NULL,
    capacite INTEGER,
    date_creation TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_salles_etablissement_nom ON salles(etablissement_id, lower(nom));
