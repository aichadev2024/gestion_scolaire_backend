-- Le surveillant général gère la discipline (retards, absences, tenue non portée, refus de
-- travail...) et le suivi de comportement des élèves — restreint par niveau via
-- niveau_supervise_id comme les autres comptes transverses (Directeur, Secrétaire, Comptable...).
insert into roles (nom) values ('SURVEILLANT_GENERAL')
on conflict (nom) do nothing;
