-- Date de la dernière relance de paiement envoyée aux parents d'un élève (limite anti-spam et
-- affichage « relancé le … »). En production Hibernate (ddl-auto=update) ajoute la colonne tout seul.
ALTER TABLE eleves ADD COLUMN derniere_relance_paiement TIMESTAMP;
