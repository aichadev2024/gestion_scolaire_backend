-- Niveau générique pour les groupes de crèche (Pouponnière, Trotteurs, Moyens, Grands…),
-- qui n'entrent pas dans le découpage Maternelle/Primaire/Collège/Lycée des écoles.
insert into niveaux (nom) values ('Crèche')
on conflict (nom) do nothing;
