# Image dédiée à la tâche planifiée de sauvegarde — indépendante de l'image
# principale du backend (Dockerfile à la racine du dépôt), volontairement minimale.
# Base postgres:18-alpine (pas alpine:3.20 nu) : Neon tourne en Postgres 18, et
# pg_dump refuse de dumper un serveur plus récent que lui ("server version
# mismatch") — cette image fournit déjà le pg_dump correspondant.
FROM postgres:18-alpine

RUN apk add --no-cache bash rclone ca-certificates tzdata

WORKDIR /app
COPY backup.sh /app/backup.sh
RUN chmod +x /app/backup.sh

ENTRYPOINT ["/app/backup.sh"]
