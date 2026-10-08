-- En-tête officiel des documents (bulletins, rapports) : rive et CAP de l'école. En production
-- Hibernate (ddl-auto=update) ajoute les colonnes tout seul.
ALTER TABLE etablissements ADD COLUMN rive VARCHAR(50);
ALTER TABLE etablissements ADD COLUMN cap VARCHAR(100);
