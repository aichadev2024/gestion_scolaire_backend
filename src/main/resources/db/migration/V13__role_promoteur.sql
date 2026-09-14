-- Le promoteur est le propriétaire/investisseur de l'établissement (distinct du directeur qui
-- gère l'opérationnel) : accès en lecture seule aux comptes du personnel et aux statistiques
-- financières, exclusivement depuis l'application mobile.
insert into roles (nom) values ('PROMOTEUR')
on conflict (nom) do nothing;
