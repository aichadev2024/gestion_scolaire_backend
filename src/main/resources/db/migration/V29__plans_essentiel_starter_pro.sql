-- Plans définitifs : ESSENTIEL (200 élèves, sans appli mobile), STARTER (300) et PRO (illimité).
-- Pour une base neuve (Flyway activé) : transforme les paliers provisoires créés par V28.
-- (En production Flyway est désactivé : TarifPlanServiceImpl.assurerPlansParDefaut fait l'équivalent.)
ALTER TABLE tarifs_plan ADD COLUMN mobile_inclus BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE tarifs_plan SET code = 'ESSENTIEL', libelle = 'Essentiel', mobile_inclus = FALSE WHERE code = 'PLAN_200';
UPDATE tarifs_plan SET code = 'STARTER', libelle = 'Starter' WHERE code = 'PLAN_300';
UPDATE tarifs_plan SET code = 'PRO', libelle = 'Pro' WHERE code = 'ILLIMITE';

-- Une école ne passe jamais automatiquement sur ESSENTIEL (elle perdrait l'appli mobile).
UPDATE etablissements SET plan_tarifaire = CASE plan_tarifaire
    WHEN 'PLAN_200' THEN 'STARTER'
    WHEN 'PLAN_300' THEN 'STARTER'
    WHEN 'ILLIMITE' THEN 'PRO'
    ELSE plan_tarifaire
END;
