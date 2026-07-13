-- Se ejecuta solo en el primer arranque del contenedor (volumen vacío).
-- El rol indicore_app es dueño de la BD; el esquema queda bajo su control para Flyway y las migraciones.
CREATE SCHEMA IF NOT EXISTS indicolors AUTHORIZATION indicore_app;
GRANT ALL ON SCHEMA indicolors TO indicore_app;
