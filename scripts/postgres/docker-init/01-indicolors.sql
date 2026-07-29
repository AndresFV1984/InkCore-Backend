-- Bootstrap Docker (solo primer arranque con volumen vacío).
-- Alineado a ScriptBD/Script_Crear_BD.sql (roles / schema / grants).
-- NO crea tablas ni seeds: eso lo hace Flyway (V1..V9) al arrancar la app.
--
-- Ya provistos por la imagen Postgres vía docker-compose:
--   POSTGRES_DB=inkcore
--   POSTGRES_USER=indicolors_owner (superusuario del contenedor)
--   POSTGRES_PASSWORD=...

-- ============================================
-- Roles (app + admin). Owner = POSTGRES_USER.
-- ============================================
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

ALTER ROLE indicolors_app WITH PASSWORD '5p5g+OQu9X6/cdi23jLKiTqSpAfgWCiS';

DO $$
BEGIN
	IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'inkcore_admin') THEN
		CREATE ROLE inkcore_admin WITH
			LOGIN
			NOSUPERUSER
			CREATEDB
			CREATEROLE
			PASSWORD 'kpDh7QcJVviWaxU91wLYyv2wIMECIUUb';
	END IF;
END $$;

ALTER ROLE inkcore_admin WITH PASSWORD 'kpDh7QcJVviWaxU91wLYyv2wIMECIUUb';

-- Dueño lógico de la BD + herencia del owner de esquema (como en Script_Crear_BD)
ALTER DATABASE inkcore OWNER TO inkcore_admin;
GRANT indicolors_owner TO inkcore_admin;

-- Event trigger: inkcore_admin recibe ALL sobre esquemas nuevos
CREATE OR REPLACE FUNCTION inkcore_grant_admin_on_new_schema()
RETURNS event_trigger AS $$
DECLARE
	obj record;
BEGIN
	FOR obj IN SELECT * FROM pg_event_trigger_ddl_commands()
		WHERE command_tag = 'CREATE SCHEMA'
	LOOP
		EXECUTE format('GRANT ALL ON SCHEMA %I TO inkcore_admin', obj.schema_name);
	END LOOP;
END;
$$ LANGUAGE plpgsql;

DROP EVENT TRIGGER IF EXISTS inkcore_new_schema_trigger;
CREATE EVENT TRIGGER inkcore_new_schema_trigger
	ON ddl_command_end
	WHEN TAG IN ('CREATE SCHEMA')
	EXECUTE FUNCTION inkcore_grant_admin_on_new_schema();

-- ============================================
-- Schema + extensión (pgcrypto también en V1 Flyway; idempotente)
-- ============================================
CREATE SCHEMA IF NOT EXISTS indicolors AUTHORIZATION indicolors_owner;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

GRANT ALL PRIVILEGES ON DATABASE inkcore TO indicolors_owner;
GRANT CONNECT ON DATABASE inkcore TO indicolors_app;
GRANT CONNECT ON DATABASE inkcore TO inkcore_admin;

GRANT ALL PRIVILEGES ON SCHEMA indicolors TO indicolors_owner;
GRANT ALL PRIVILEGES ON SCHEMA indicolors TO inkcore_admin;
GRANT USAGE ON SCHEMA indicolors TO indicolors_app;

-- Defaults genéricos del schema
ALTER DEFAULT PRIVILEGES IN SCHEMA indicolors
	GRANT ALL PRIVILEGES ON TABLES TO indicolors_owner;
ALTER DEFAULT PRIVILEGES IN SCHEMA indicolors
	GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO indicolors_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA indicolors
	GRANT ALL PRIVILEGES ON SEQUENCES TO indicolors_owner;
ALTER DEFAULT PRIVILEGES IN SCHEMA indicolors
	GRANT USAGE, SELECT ON SEQUENCES TO indicolors_app;

-- Objetos futuros creados por Flyway / DDL con indicolors_owner
ALTER DEFAULT PRIVILEGES FOR ROLE indicolors_owner IN SCHEMA indicolors
	GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO indicolors_app;
ALTER DEFAULT PRIVILEGES FOR ROLE indicolors_owner IN SCHEMA indicolors
	GRANT USAGE, SELECT ON SEQUENCES TO indicolors_app;
ALTER DEFAULT PRIVILEGES FOR ROLE indicolors_owner IN SCHEMA indicolors
	GRANT ALL PRIVILEGES ON TABLES TO inkcore_admin;
ALTER DEFAULT PRIVILEGES FOR ROLE indicolors_owner IN SCHEMA indicolors
	GRANT ALL PRIVILEGES ON SEQUENCES TO inkcore_admin;

-- Search path
ALTER DATABASE inkcore SET search_path TO indicolors, public;
ALTER ROLE indicolors_app SET search_path TO indicolors;

-- Endurecimiento: la app no usa schema public
REVOKE CREATE ON SCHEMA public FROM indicolors_app;
REVOKE ALL ON SCHEMA public FROM indicolors_app;
