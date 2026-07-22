-- Se ejecuta solo en el primer arranque del contenedor (volumen vacío).
-- Roles: indicolors_owner (POSTGRES_USER / DDL + Flyway) e indicolors_app (runtime).
-- No existe indicolors_migrator — Flyway usa indicolors_owner (ver Script_Crear_BD).

DO $$
BEGIN
	IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'indicolors_app') THEN
		CREATE ROLE indicolors_app WITH
			LOGIN
			NOSUPERUSER
			NOCREATEDB
			NOCREATEROLE
			PASSWORD 'indicolors2026!';
	END IF;
END $$;

CREATE SCHEMA IF NOT EXISTS indicolors AUTHORIZATION indicolors_owner;
GRANT ALL ON SCHEMA indicolors TO indicolors_owner;

GRANT CONNECT ON DATABASE inkcore TO indicolors_app;
GRANT USAGE ON SCHEMA indicolors TO indicolors_app;

ALTER ROLE indicolors_app SET search_path TO indicolors;

REVOKE CREATE ON SCHEMA public FROM indicolors_app;
REVOKE ALL ON SCHEMA public FROM indicolors_app;

ALTER DEFAULT PRIVILEGES FOR ROLE indicolors_owner IN SCHEMA indicolors
	GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO indicolors_app;

ALTER DEFAULT PRIVILEGES FOR ROLE indicolors_owner IN SCHEMA indicolors
	GRANT USAGE, SELECT ON SEQUENCES TO indicolors_app;
