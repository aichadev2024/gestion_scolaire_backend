-- Abonnements par palier d'effectif : ESSENTIEL (200 élèves, sans appli mobile), STARTER (300 élèves)
-- et PRO (illimité). Les deux derniers existaient déjà (limités jusqu'ici par le nombre d'enseignants).
ALTER TABLE tarifs_plan ADD COLUMN max_eleves INTEGER;
ALTER TABLE tarifs_plan ADD COLUMN libelle VARCHAR(60);
ALTER TABLE tarifs_plan ADD COLUMN mobile_inclus BOOLEAN NOT NULL DEFAULT TRUE;

-- Les prix existants de STARTER/PRO sont conservés : à ajuster par le super-admin (Paramètres).
UPDATE tarifs_plan SET libelle = 'Starter', max_eleves = 300, max_enseignants = NULL, mobile_inclus = TRUE WHERE code = 'STARTER';
UPDATE tarifs_plan SET libelle = 'Pro', max_eleves = NULL, max_enseignants = NULL, mobile_inclus = TRUE WHERE code = 'PRO';
INSERT INTO tarifs_plan (code, prix_mensuel, max_eleves, max_enseignants, libelle, mobile_inclus)
VALUES ('ESSENTIEL', 15000, 200, NULL, 'Essentiel', FALSE)
ON CONFLICT (code) DO NOTHING;

-- Établissements existants : jamais rétrogradés vers ESSENTIEL (ils perdraient l'appli mobile qu'ils
-- utilisent). Ceux sur PRO restent PRO ; les autres passent sur STARTER si leur effectif actuel tient
-- dans 300 élèves, sinon sur PRO — personne ne se retrouve au-dessus de sa limite à la migration.
UPDATE etablissements e SET plan_tarifaire = CASE
    WHEN e.plan_tarifaire = 'PRO' THEN 'PRO'
    WHEN (SELECT count(*) FROM eleves x WHERE x.etablissement_id = e.id AND coalesce(x.statut, 'ACTIF') <> 'ARCHIVE') <= 300 THEN 'STARTER'
    ELSE 'PRO'
END;
