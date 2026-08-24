-- Rol runtime de la app (indicolors_app).
-- En instalaciones nuevas suele crearse en bootstrap (docker-init / Script_Crear_BD).
-- Idempotente: cubre volúmenes Postgres de Docker creados antes de scripts/postgres/docker-init.

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

GRANT CONNECT ON DATABASE inkcore TO indicolors_app;
GRANT USAGE ON SCHEMA indicolors TO indicolors_app;
ALTER ROLE indicolors_app SET search_path TO indicolors;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA indicolors TO indicolors_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA indicolors TO indicolors_app;

ALTER DEFAULT PRIVILEGES FOR ROLE indicolors_owner IN SCHEMA indicolors
	GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO indicolors_app;
ALTER DEFAULT PRIVILEGES FOR ROLE indicolors_owner IN SCHEMA indicolors
	GRANT USAGE, SELECT ON SEQUENCES TO indicolors_app;

REVOKE CREATE ON SCHEMA public FROM indicolors_app;
REVOKE ALL ON SCHEMA public FROM indicolors_app;
