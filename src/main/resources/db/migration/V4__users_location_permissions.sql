-- Ubicación geográfica y permisos de usuario (alineado al formulario de alta/edición)

ALTER TABLE indicolors.users
    ADD COLUMN IF NOT EXISTS department VARCHAR(100);

ALTER TABLE indicolors.users
    ADD COLUMN IF NOT EXISTS city VARCHAR(100);

UPDATE indicolors.users
SET department = COALESCE(NULLIF(TRIM(department), ''), 'Antioquia')
WHERE department IS NULL OR TRIM(department) = '';

UPDATE indicolors.users
SET city = COALESCE(NULLIF(TRIM(city), ''), 'Medellín')
WHERE city IS NULL OR TRIM(city) = '';

ALTER TABLE indicolors.users
    ALTER COLUMN department SET NOT NULL;

ALTER TABLE indicolors.users
    ALTER COLUMN city SET NOT NULL;

ALTER TABLE indicolors.users
    ALTER COLUMN contact DROP NOT NULL;

ALTER TABLE indicolors.users
    ALTER COLUMN address DROP NOT NULL;

COMMENT ON COLUMN indicolors.users.department IS 'Departamento (ubicación)';
COMMENT ON COLUMN indicolors.users.city IS 'Ciudad / municipio';

CREATE INDEX IF NOT EXISTS idx_users_department ON indicolors.users (department);
CREATE INDEX IF NOT EXISTS idx_users_city ON indicolors.users (city);

-- Rol Operador (formulario frontend)
INSERT INTO indicolors.roles (role_id, code, name)
SELECT 'b1ffbc99-9c0b-4ef8-bb6d-6bb9bd380a22'::uuid, 'OPERADOR', 'Operador'
WHERE NOT EXISTS (
    SELECT 1 FROM indicolors.roles WHERE UPPER(code) = 'OPERADOR'
);

CREATE TABLE IF NOT EXISTS indicolors.permissions (
    permission_id UUID NOT NULL,
    code          VARCHAR(80) NOT NULL,
    name          VARCHAR(200) NOT NULL,
    CONSTRAINT permissions_pkey PRIMARY KEY (permission_id),
    CONSTRAINT permissions_code_unique UNIQUE (code)
);

COMMENT ON TABLE indicolors.permissions IS 'Catálogo de permisos finos del sistema';

CREATE TABLE IF NOT EXISTS indicolors.user_permissions (
    user_id       VARCHAR(64) NOT NULL,
    permission_id UUID NOT NULL,
    CONSTRAINT user_permissions_pkey PRIMARY KEY (user_id, permission_id),
    CONSTRAINT fk_user_permissions_user
        FOREIGN KEY (user_id) REFERENCES indicolors.users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES indicolors.permissions (permission_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_permissions_permission
    ON indicolors.user_permissions (permission_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'roles_pkey' AND conrelid = 'indicolors.roles'::regclass
    ) THEN
        ALTER TABLE indicolors.roles ADD CONSTRAINT roles_pkey PRIMARY KEY (role_id);
    END IF;
EXCEPTION
    WHEN others THEN
        NULL;
END $$;

INSERT INTO indicolors.permissions (permission_id, code, name) VALUES
    ('c1000001-0000-4000-8000-000000000001'::uuid, 'VER_DASHBOARD', 'Ver dashboard e inicio'),
    ('c1000001-0000-4000-8000-000000000002'::uuid, 'CREAR_EDITAR_ORDENES_PRODUCCION', 'Crear y editar órdenes de producción'),
    ('c1000001-0000-4000-8000-000000000003'::uuid, 'GESTIONAR_ESTADO_GLOBAL_PRODUCCION', 'Gestionar estado global de producción'),
    ('c1000001-0000-4000-8000-000000000004'::uuid, 'MARCAR_PREPRENSA_EN_PROCESO', 'Marcar preprensa en proceso'),
    ('c1000001-0000-4000-8000-000000000005'::uuid, 'MARCAR_IMPRESION_EN_PROCESO', 'Marcar impresión en proceso'),
    ('c1000001-0000-4000-8000-000000000006'::uuid, 'MARCAR_ACABADOS_EN_PROCESO', 'Marcar acabados en proceso'),
    ('c1000001-0000-4000-8000-000000000007'::uuid, 'VER_PEDIDOS', 'Ver pedidos'),
    ('c1000001-0000-4000-8000-000000000008'::uuid, 'VER_ORDENES_PRODUCCION', 'Ver órdenes de producción'),
    ('c1000001-0000-4000-8000-000000000009'::uuid, 'VER_ESTADO_ORDENES_PRODUCCION', 'Ver estado de órdenes de producción'),
    ('c1000001-0000-4000-8000-00000000000a'::uuid, 'MARCAR_ETAPAS_PRODUCCION_EN_PROCESO', 'Marcar etapas de producción en proceso'),
    ('c1000001-0000-4000-8000-00000000000b'::uuid, 'MARCAR_CORTE_PAPEL_EN_PROCESO', 'Marcar corte de papel en proceso'),
    ('c1000001-0000-4000-8000-00000000000c'::uuid, 'MARCAR_TERMINADOS_EN_PROCESO', 'Marcar terminados en proceso'),
    ('c1000001-0000-4000-8000-00000000000d'::uuid, 'GESTIONAR_MI_TRABAJO_PRODUCCION', 'Gestionar mi trabajo en producción'),
    ('c1000001-0000-4000-8000-00000000000e'::uuid, 'GESTIONAR_PEDIDOS', 'Gestionar pedidos')
ON CONFLICT (permission_id) DO NOTHING;
