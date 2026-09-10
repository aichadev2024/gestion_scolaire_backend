-- =====================================================================
-- V4 — Fusion du rôle ADMIN dans DIRECTEUR
-- =====================================================================
-- Dans une école, l'« administrateur » de l'établissement EST le directeur
-- (le censeur au lycée). On supprime le rôle ADMIN : DIRECTEUR reprend
-- l'intégralité de ses droits (voir les @PreAuthorize côté backend).
--
-- Idempotent : ne fait rien si le rôle ADMIN n'existe plus.
-- =====================================================================

-- 1. S'assurer que DIRECTEUR existe
insert into roles (nom)
select 'DIRECTEUR'
where not exists (select 1 from roles where nom = 'DIRECTEUR');

-- 2. Rattacher les utilisateurs ADMIN au rôle DIRECTEUR
update utilisateurs
set role_id = (select id from roles where nom = 'DIRECTEUR')
where role_id = (select id from roles where nom = 'ADMIN');

-- 3. Supprimer le rôle ADMIN
delete from roles where nom = 'ADMIN';
