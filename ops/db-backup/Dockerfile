# Image dédiée à la tâche planifiée de sauvegarde — indépendante de l'image
# principale du backend (Dockerfile à la racine du dépôt), volontairement minimale.
FROM alpine:3.20

RUN apk add --no-cache bash postgresql16-client rclone ca-certificates tzdata

WORKDIR /app
COPY backup.sh /app/backup.sh
RUN chmod +x /app/backup.sh

ENTRYPOINT ["/app/backup.sh"]
