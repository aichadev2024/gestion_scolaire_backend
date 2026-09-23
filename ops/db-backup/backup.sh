#!/bin/sh
# Sauvegarde quotidienne de la base Neon vers Cloudflare R2 — indépendante du PITR
# de Neon (qui reste la première ligne de défense, restauration en quelques secondes).
# Cette copie protège contre un problème côté Neon lui-même (compte, facturation,
# panne du fournisseur) : elle vit dans un bucket R2 séparé, jamais public.
set -eu

: "${PGHOST:?PGHOST manquant}"
: "${PGDATABASE:?PGDATABASE manquant}"
: "${PGUSER:?PGUSER manquant}"
: "${PGPASSWORD:?PGPASSWORD manquant}"
: "${BACKUP_R2_ACCOUNT_ID:?BACKUP_R2_ACCOUNT_ID manquant}"
: "${BACKUP_R2_ACCESS_KEY_ID:?BACKUP_R2_ACCESS_KEY_ID manquant}"
: "${BACKUP_R2_SECRET_ACCESS_KEY:?BACKUP_R2_SECRET_ACCESS_KEY manquant}"
: "${BACKUP_R2_BUCKET:?BACKUP_R2_BUCKET manquant}"

export PGPORT="${PGPORT:-5432}"
# Neon exige TLS.
export PGSSLMODE="${PGSSLMODE:-require}"

# Config rclone entièrement par variables d'environnement — pas de fichier de conf
# à gérer dans l'image. "r2" est le nom du "remote" utilisé plus bas.
export RCLONE_CONFIG_R2_TYPE=s3
export RCLONE_CONFIG_R2_PROVIDER=Cloudflare
export RCLONE_CONFIG_R2_ACCESS_KEY_ID="$BACKUP_R2_ACCESS_KEY_ID"
export RCLONE_CONFIG_R2_SECRET_ACCESS_KEY="$BACKUP_R2_SECRET_ACCESS_KEY"
export RCLONE_CONFIG_R2_ENDPOINT="https://${BACKUP_R2_ACCOUNT_ID}.r2.cloudflarestorage.com"
export RCLONE_CONFIG_R2_ACL=private
export RCLONE_CONFIG_R2_NO_CHECK_BUCKET=true

STAMP="$(date -u +%Y-%m-%d-%H%M%S)"
FILE="netaa-ecole-${STAMP}.dump"
TMP="/tmp/${FILE}"

echo "[backup] $(date -u +%FT%TZ) — export de ${PGDATABASE}@${PGHOST} vers ${FILE}"
# Format « custom » : déjà compressé, restaurable en entier ou table par table
# avec pg_restore (contrairement à un simple .sql.gz).
pg_dump --format=custom --no-owner --no-privileges --file="$TMP"
echo "[backup] dump terminé ($(du -h "$TMP" | cut -f1))"

echo "[backup] envoi vers r2://${BACKUP_R2_BUCKET}/${FILE}"
rclone copyto "$TMP" "r2:${BACKUP_R2_BUCKET}/${FILE}"
rm -f "$TMP"

RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-35}"
echo "[backup] purge des sauvegardes de plus de ${RETENTION_DAYS} jours"
rclone delete "r2:${BACKUP_R2_BUCKET}" --min-age "${RETENTION_DAYS}d" \
  || echo "[backup] purge : rien à supprimer, ou erreur non bloquante"

echo "[backup] terminé avec succès : ${FILE}"
