-- =====================================================================
-- V2 — Données de référence (rôles, niveaux d'enseignement)
-- Idempotent : réexécutable sans erreur (ON CONFLICT DO NOTHING).
-- Doit rester cohérent avec core/config/DataInitializer.java
-- =====================================================================

insert into roles (nom) values
    ('SUPER_ADMIN'),
    ('ADMIN'),
    ('DIRECTEUR'),
    ('SECRETAIRE'),
    ('COMPTABLE'),
    ('ENSEIGNANT'),
    ('ELEVE'),
    ('PARENT')
on conflict (nom) do nothing;

insert into niveaux (nom) values
    ('Maternelle'),
    ('Primaire'),
    ('Collège'),
    ('Lycée')
on conflict (nom) do nothing;
