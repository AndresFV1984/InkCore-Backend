-- Relación N:M roles ↔ permisos + semilla
-- Script manual de consulta/ajuste: scripts/postgres/assign-role-permissions.sql

CREATE TABLE IF NOT EXISTS indicolors.role_permissions (
    role_id       UUID                         NOT NULL,
    permission_id UUID                         NOT NULL,
    granted_at    TIMESTAMP WITHOUT TIME ZONE  NOT NULL DEFAULT now(),
    CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id),
    CONSTRAINT role_permissions_role_fk
        FOREIGN KEY (role_id) REFERENCES indicolors.roles (role_id) ON DELETE CASCADE,
    CONSTRAINT role_permissions_permission_fk
        FOREIGN KEY (permission_id) REFERENCES indicolors.permissions (permission_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON indicolors.role_permissions (permission_id);

COMMENT ON TABLE indicolors.role_permissions IS 'Relación N:M entre roles y permisos';

-- Administrador: todos los permisos
INSERT INTO indicolors.role_permissions (role_id, permission_id)
SELECT 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid, p.permission_id
FROM indicolors.permissions p
ON CONFLICT DO NOTHING;

-- Operador: permisos operativos de producción
INSERT INTO indicolors.role_permissions (role_id, permission_id)
SELECT 'b1ffbc99-9c0b-4ef8-bb6d-6bb9bd380a22'::uuid, p.permission_id
FROM indicolors.permissions p
WHERE p.code IN (
    'production.orders.create_edit',
    'production.orders.view',
    'production.status.view',
    'production.stages.mark',
    'production.preprensa.mark',
    'production.corte.mark',
    'production.impresion.mark',
    'production.terminados.mark',
    'production.acabados.mark',
    'production.mywork.manage'
)
ON CONFLICT DO NOTHING;
