-- Abonnements par palier d'effectif d'élèves (remplace STARTER/PRO, qui limitaient les enseignants).
ALTER TABLE tarifs_plan ADD COLUMN max_eleves INTEGER;
ALTER TABLE tarifs_plan ADD COLUMN libelle VARCHAR(60);

INSERT INTO tarifs_plan (code, prix_mensuel, max_eleves, max_enseignants, libelle) VALUES
    ('PLAN_200', 15000, 200, NULL, 'Jusqu''à 200 élèves'),
    ('PLAN_300', 25000, 300, NULL, 'Jusqu''à 300 élèves'),
    ('ILLIMITE', 35000, NULL, NULL, 'Élèves illimités');

-- Chaque établissement existant est rattaché au plus petit palier qui couvre son effectif
-- actuel : aucun établissement ne se retrouve au-dessus de sa limite à la migration.
UPDATE etablissements e SET plan_tarifaire = CASE
    WHEN (SELECT count(*) FROM eleves x WHERE x.etablissement_id = e.id AND coalesce(x.statut, 'ACTIF') <> 'ARCHIVE') <= 200 THEN 'PLAN_200'
    WHEN (SELECT count(*) FROM eleves x WHERE x.etablissement_id = e.id AND coalesce(x.statut, 'ACTIF') <> 'ARCHIVE') <= 300 THEN 'PLAN_300'
    ELSE 'ILLIMITE'
END;

DELETE FROM tarifs_plan WHERE code IN ('STARTER', 'PRO');
