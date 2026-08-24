# Roles/schema/grants en primer arranque (volumen vacío).
# El cliente no monta ./scripts: todo va en la imagen Hub.
FROM postgres:16-alpine
COPY docker-init/01-indicolors.sql /docker-entrypoint-initdb.d/01-indicolors.sql
