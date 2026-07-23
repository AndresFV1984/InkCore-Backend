-- Se ejecuta solo en el primer arranque del contenedor (volumen vacío).
-- Roles: indicolors_owner (POSTGRES_USER / DDL + Flyway) e indicolors_app (runtime).
-- Passwords alineados a application-*.yaml / Script_Crear_BD (solo para entorno Docker local).
-- Las tablas las crea Flyway al arrancar la app (V1..V8).

DO $$
BEGIN
	IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'indicolors_app') THEN
		CREATE ROLE indicolors_app WITH
			LOGIN
			NOSUPERUSER
			NOCREATEDB
			NOCREATEROLE
			PASSWORD '5p5g+OQu9X6/cdi23jLKiTqSpAfgWCiS';
	END IF;
END $$;

-- Si el rol ya existía (re-init parcial), asegura el password de Docker local
ALTER ROLE indicolors_app WITH PASSWORD '5p5g+OQu9X6/cdi23jLKiTqSpAfgWCiS';

CREATE SCHEMA IF NOT EXISTS indicolors AUTHORIZATION indicolors_owner;
GRANT ALL ON SCHEMA indicolors TO indicolors_owner;

GRANT CONNECT ON DATABASE inkcore TO indicolors_app;
GRANT USAGE ON SCHEMA indicolors TO indicolors_app;

ALTER ROLE indicolors_app SET search_path TO indicolors;
ALTER DATABASE inkcore SET search_path TO indicolors, public;

REVOKE CREATE ON SCHEMA public FROM indicolors_app;
REVOKE ALL ON SCHEMA public FROM indicolors_app;

ALTER DEFAULT PRIVILEGES FOR ROLE indicolors_owner IN SCHEMA indicolors
	GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO indicolors_app;

ALTER DEFAULT PRIVILEGES FOR ROLE indicolors_owner IN SCHEMA indicolors
	GRANT USAGE, SELECT ON SEQUENCES TO indicolors_app;
