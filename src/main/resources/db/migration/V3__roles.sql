-- Catálogo de roles (por empresa)
CREATE TABLE IF NOT EXISTS indicolors.roles (
    role_id      UUID                         NOT NULL DEFAULT gen_random_uuid(),
    company_id   VARCHAR(64)                  NOT NULL,
    name         VARCHAR(100)                 NOT NULL,
    description  VARCHAR(255),
    state        BOOLEAN                      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP WITHOUT TIME ZONE  NOT NULL DEFAULT now(),
    CONSTRAINT roles_pkey PRIMARY KEY (role_id),
    CONSTRAINT roles_name_company_unique UNIQUE (company_id, name),
    CONSTRAINT roles_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id)
);

CREATE INDEX IF NOT EXISTS idx_roles_company_id ON indicolors.roles (company_id);
CREATE INDEX IF NOT EXISTS idx_roles_state ON indicolors.roles (state);

COMMENT ON TABLE indicolors.roles IS 'Catálogo de roles del sistema (ej. Operador, Administrador)';
COMMENT ON COLUMN indicolors.roles.company_id IS 'Identificador de la empresa dueña del rol';
COMMENT ON COLUMN indicolors.roles.state IS 'True=Activo, False=Inactivo';

INSERT INTO indicolors.roles (role_id, company_id, name, description, state)
VALUES
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid,
        'company-seed-001',
        'Administrador',
        'Acceso administrativo completo',
        TRUE
    ),
    (
        'b1ffbc99-9c0b-4ef8-bb6d-6bb9bd380a22'::uuid,
        'company-seed-001',
        'Operador',
        'Rol operativo de planta de producción',
        TRUE
    )
ON CONFLICT (role_id) DO NOTHING;

