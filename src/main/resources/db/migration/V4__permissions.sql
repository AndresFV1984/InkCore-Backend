-- Catálogo global de permisos
CREATE TABLE IF NOT EXISTS indicolors.permissions (
    permission_id UUID                         NOT NULL DEFAULT gen_random_uuid(),
    code          VARCHAR(100)                 NOT NULL,
    name          VARCHAR(150)                 NOT NULL,
    module        VARCHAR(100),
    description   VARCHAR(255),
    created_at    TIMESTAMP WITHOUT TIME ZONE  NOT NULL DEFAULT now(),
    CONSTRAINT permissions_pkey PRIMARY KEY (permission_id),
    CONSTRAINT permissions_code_unique UNIQUE (code)
);

CREATE INDEX IF NOT EXISTS idx_permissions_module ON indicolors.permissions (module);

COMMENT ON TABLE indicolors.permissions IS 'Catálogo global de permisos disponibles en el sistema';
COMMENT ON COLUMN indicolors.permissions.code IS 'Código único del permiso (ej. production.orders.create_edit)';
COMMENT ON COLUMN indicolors.permissions.name IS 'Nombre visible del permiso';
COMMENT ON COLUMN indicolors.permissions.module IS 'Módulo/agrupación funcional (ej. produccion, pedidos)';

INSERT INTO indicolors.permissions (permission_id, code, name, module) VALUES
    ('c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a01'::uuid, 'dashboard.view',                'Ver dashboard e inicio',                  'dashboard'),
    ('c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a02'::uuid, 'production.orders.create_edit', 'Crear y editar órdenes de producción',    'produccion'),
    ('c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a03'::uuid, 'production.orders.view',        'Ver órdenes de producción',               'produccion'),
    ('c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a04'::uuid, 'production.status.manage',      'Gestionar estado global de producción',   'produccion'),
    ('c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a05'::uuid, 'production.status.view',        'Ver estado de órdenes de producción',     'produccion'),
    ('c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a06'::uuid, 'production.stages.mark',        'Marcar etapas de producción en proceso',  'produccion'),
    ('c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a07'::uuid, 'production.preprensa.mark',     'Marcar preprensa en proceso',             'produccion'),
    ('c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a08'::uuid, 'production.corte.mark',         'Marcar corte de papel en proceso',        'produccion'),
    ('c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a09'::uuid, 'production.impresion.mark',     'Marcar impresión en proceso',             'produccion'),
    ('c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a0a'::uuid, 'production.terminados.mark',    'Marcar terminados en proceso',            'produccion'),
    ('c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a0b'::uuid, 'production.acabados.mark',      'Marcar acabados en proceso',              'produccion'),
    ('c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a0c'::uuid, 'production.mywork.manage',      'Gestionar mi trabajo en producción',      'produccion'),
    ('c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a0d'::uuid, 'orders.view',                   'Ver pedidos',                             'pedidos'),
    ('c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a0e'::uuid, 'orders.manage',                 'Gestionar pedidos',                       'pedidos')
ON CONFLICT (permission_id) DO NOTHING;

