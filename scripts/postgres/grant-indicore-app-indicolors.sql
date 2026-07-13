-- Ejecutar como indicore_admin (o postgres) en la base indicore.
-- Necesario tras "Scrip Crear BD.sql", que solo otorga USAGE (no CREATE) a indicore_app.

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'indicore_app') THEN
    RAISE EXCEPTION 'Crear primero el rol indicore_app o ajustar el nombre en este script';
  END IF;
END $$;

CREATE SCHEMA IF NOT EXISTS indicolors AUTHORIZATION indicore_app;

GRANT USAGE, CREATE ON SCHEMA indicolors TO indicore_app;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA indicolors TO indicore_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA indicolors TO indicore_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA indicolors GRANT ALL ON TABLES TO indicore_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA indicolors GRANT ALL ON SEQUENCES TO indicore_app;

-- Si el esquema ya existía con otro dueño:
ALTER SCHEMA indicolors OWNER TO indicore_app;

ALTER TABLE indicolors.users OWNER TO indicore_app;
