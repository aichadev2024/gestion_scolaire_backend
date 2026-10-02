-- Nom que chaque école donne à son enseignement professionnel (ex. « IFTICA » pour le lycée
-- Ba Fanta Coulibaly). Le niveau « Enseignement Professionnel » du catalogue est créé au démarrage
-- par DataInitializer ; en production Hibernate (ddl-auto=update) ajoute la colonne tout seul.
ALTER TABLE etablissements ADD COLUMN nom_enseignement_professionnel VARCHAR(100);
