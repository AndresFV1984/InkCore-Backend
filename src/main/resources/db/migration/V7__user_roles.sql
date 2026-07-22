-- Relación N:M usuarios ↔ roles
CREATE TABLE IF NOT EXISTS indicolors.user_roles (
    user_id     VARCHAR(64)                 NOT NULL,
    role_id     UUID                        NOT NULL,
    assigned_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id),
    CONSTRAINT user_roles_user_fk
        FOREIGN KEY (user_id) REFERENCES indicolors.users (user_id) ON DELETE CASCADE,
    CONSTRAINT user_roles_role_fk
        FOREIGN KEY (role_id) REFERENCES indicolors.roles (role_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON indicolors.user_roles (role_id);

COMMENT ON TABLE indicolors.user_roles IS 'Relación N:M entre usuarios y roles';

-- Asignar rol Administrador al usuario semilla
INSERT INTO indicolors.user_roles (user_id, role_id)
VALUES (
    'seed-cfg-9f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid
)
ON CONFLICT DO NOTHING;

