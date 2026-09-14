-- Portée d'un compte (Directeur, Secrétaire, Comptable...) restreinte à un seul niveau
-- (Crèche/Maternelle/Primaire/Collège/Lycée) — null = aucune restriction, accès à tout
-- l'établissement (cas des comptes transverses : comptable, secrétariat général...).
ALTER TABLE utilisateurs ADD COLUMN niveau_supervise_id INTEGER REFERENCES niveaux(id);
