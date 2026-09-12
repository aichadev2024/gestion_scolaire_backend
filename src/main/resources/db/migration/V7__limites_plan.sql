ALTER TABLE tarifs_plan ADD COLUMN max_enseignants INTEGER;

UPDATE tarifs_plan SET max_enseignants = 5 WHERE code = 'STARTER';
