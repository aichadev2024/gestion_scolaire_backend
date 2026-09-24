-- Réponse du destinataire à une notification (justification d'absence, ou tout autre message) —
-- stockée directement sur la notification d'origine pour un affichage simple côté appli.
ALTER TABLE notifications ADD COLUMN reponse_contenu TEXT;
ALTER TABLE notifications ADD COLUMN reponse_date TIMESTAMP;
