-- Statut pédagogique de l'élève dans sa classe actuelle : REGULIER (suit normalement)
-- ou REDOUBLANT (reprend son année) — distinct de `statut` (actif/archivé) et de
-- `statut_inscription` (dossier administratif). Sert notamment au passage en classe
-- supérieure : un redoublant reste dans sa classe au lieu d'être promu.
ALTER TABLE eleves ADD COLUMN statut_pedagogique VARCHAR(20) NOT NULL DEFAULT 'REGULIER';
