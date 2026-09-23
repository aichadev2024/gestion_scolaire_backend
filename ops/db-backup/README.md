# Sauvegarde quotidienne de la base — Cloudflare R2

Copie indépendante de la base Neon, en plus du PITR déjà actif côté Neon. Objectif :
si Neon a un problème (compte, facturation, panne), il existe une copie ailleurs.

Fonctionnement : une tâche planifiée (Render **Cron Job**) exécute `backup.sh` une
fois par jour. Le script exporte la base avec `pg_dump` (format « custom », déjà
compressé) et l'envoie dans un bucket Cloudflare R2 **privé** (jamais public — le
bucket `netaa-ecole-media` déjà utilisé pour les photos ne convient pas, il est
public). Les sauvegardes de plus de 35 jours sont supprimées automatiquement.

## 1. Créer un bucket R2 privé dédié

Dans le tableau de bord Cloudflare (le même compte que pour `netaa-ecole-media`) :

1. R2 ▸ Create bucket ▸ nom `netaa-ecole-backups` (ou autre nom, à adapter dans les
   variables ci-dessous).
2. **Ne pas** activer l'accès public — contrairement au bucket média, celui-ci
   contient des données personnelles d'élèves, il doit rester privé.

## 2. Créer un jeton d'API R2 dédié

R2 ▸ Manage R2 API Tokens ▸ Create API Token :
- Permissions : **Object Read & Write**, limité au bucket `netaa-ecole-backups`
  uniquement (pas d'accès au bucket média — principe du moindre privilège).
- Note l'**Access Key ID** et la **Secret Access Key** (affichée une seule fois).

## 3. Créer le Cron Job sur Render

Render ▸ New ▸ Cron Job :
- **Repository** : ce dépôt (`gestion_scolaire_backend`).
- **Root Directory** : `ops/db-backup`
- **Dockerfile Path** : `ops/db-backup/Dockerfile` (ou laisser Render le détecter
  depuis le Root Directory).
- **Schedule** : `0 2 * * *` (02h00 UTC chaque jour — hors heures d'usage au Mali).
- **Plan** : le plus petit disponible suffit (tâche de quelques secondes).

### Variables d'environnement du Cron Job

| Variable | Valeur |
|---|---|
| `PGHOST` | hôte Postgres Neon (celui de `DATABASE_URL` du backend, sans le préfixe `jdbc:postgresql://` ni les paramètres) |
| `PGDATABASE` | nom de la base |
| `PGUSER` | `DB_USERNAME` (même valeur que le backend) |
| `PGPASSWORD` | `DB_PASSWORD` (même valeur que le backend) |
| `BACKUP_R2_ACCOUNT_ID` | même valeur que `R2_ACCOUNT_ID` du backend |
| `BACKUP_R2_ACCESS_KEY_ID` | Access Key ID du jeton créé à l'étape 2 (**pas** celui du bucket média) |
| `BACKUP_R2_SECRET_ACCESS_KEY` | Secret Access Key du même jeton |
| `BACKUP_R2_BUCKET` | `netaa-ecole-backups` |
| `BACKUP_RETENTION_DAYS` | `35` (optionnel, c'est déjà la valeur par défaut) |

> `PGHOST`/`PGDATABASE` : dans le tableau de bord Neon, l'onglet « Connection
> Details » donne aussi une chaîne au format `postgresql://user:pass@host/db` —
> `host` et `db` sont les valeurs à reprendre ici.

## 4. Vérifier

Render ▸ le Cron Job ▸ **Trigger Run** (exécution manuelle immédiate). Les logs
doivent afficher `[backup] terminé avec succès : netaa-ecole-AAAA-MM-JJ-HHMMSS.dump`.
Puis, dans Cloudflare R2 ▸ bucket `netaa-ecole-backups`, le fichier doit apparaître.

## 5. Tester une restauration (à faire une fois, puis chaque trimestre)

Ne jamais restaurer sur la base de production. Sur une base Postgres 16 jetable
(ex. un service Neon ou Docker local temporaire) :

```bash
# Télécharger le fichier .dump depuis R2 (tableau de bord ou rclone), puis :
pg_restore --clean --if-exists --no-owner --no-privileges \
  --dbname="postgresql://user:pass@host:5432/base_de_test" \
  netaa-ecole-2026-09-24-020000.dump
```

Se connecter ensuite à cette base de test avec le backend (`FLYWAY_ENABLED=false`,
`JPA_DDL_AUTO=validate`) et vérifier qu'un compte existant se connecte bien.
