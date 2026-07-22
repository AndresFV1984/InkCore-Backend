-- Relación N:M roles ↔ permisos (solo DDL).
-- La asignación de permisos a roles se hace desde BD con el script informativo:
--   scripts/postgres/assign-role-permissions.sql

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
