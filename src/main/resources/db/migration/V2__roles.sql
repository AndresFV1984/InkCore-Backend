-- Catálogo de roles (requerido para JWT y autorización)
CREATE TABLE indicolors.roles (
    role_id VARCHAR(64) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uq_roles_code UNIQUE (code)
);

COMMENT ON TABLE indicolors.roles IS 'Roles del sistema (ej. ADMINISTRADOR)';

-- Semilla: administrador (role_id fijo para usuario semilla en V3)
INSERT INTO indicolors.roles (role_id, code, name)
VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid,
    'ADMINISTRADOR',
    'Administrador'
)
ON CONFLICT (role_id) DO NOTHING;
