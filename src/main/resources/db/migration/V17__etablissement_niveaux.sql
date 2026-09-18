-- Niveaux qu'un établissement propose effectivement (ex. un lycée qui n'a que le général et le
-- professionnel, pas la crèche ni le primaire). Règle : AUCUNE ligne pour un établissement =
-- aucune restriction (comportement historique, rétrocompatible avec tous les établissements
-- existants) ; au moins une ligne = l'établissement est restreint à ces niveaux-là uniquement.
CREATE TABLE etablissement_niveaux (
    etablissement_id BIGINT NOT NULL REFERENCES etablissements(id) ON DELETE CASCADE,
    niveau_id INTEGER NOT NULL REFERENCES niveaux(id) ON DELETE CASCADE,
    PRIMARY KEY (etablissement_id, niveau_id)
);
