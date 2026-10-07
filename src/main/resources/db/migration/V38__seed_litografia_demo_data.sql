-- Datos semilla de litografia (company-seed-001) para desarrollo y demos.
-- Operadores, clientes, vendedores y cuentas bancarias.
-- Proveedores: V20__suppliers.sql | Despieces: V15__cut_layouts.sql
-- Papeles, precios, paper_cut_layouts y stock: V21-V25.
-- Password operadores: Indicore2026! (mismo hash que admin@indicolors.com)

-- ---------------------------------------------------------------------------
-- Usuarios operadores
-- ---------------------------------------------------------------------------
INSERT INTO indicolors.users (
    user_id,
    company_id,
    identification_number,
    document_type,
    name,
    mail,
    contact,
    department,
    city,
    address,
    password_hash,
    creation_date,
    state,
    token_version,
    force_password_change,
    failed_attempts
) VALUES
    (
        'operator-seed-001',
        'company-seed-001',
        '1001001001',
        'CC',
        'Operario Preprensa Litografía',
        'operario.preprensa@indicolors.com',
        '3001001001',
        'Antioquia',
        'Medellín',
        'Planta Litografía, Medellín',
        '$2a$10$WGGWmu2DjL2BnBljv70Gzu7TShx6GHdIu/.ivtnSklhahp1zWJwQi',
        CURRENT_DATE,
        TRUE,
        1,
        FALSE,
        0
    ),
    (
        'operator-seed-002',
        'company-seed-001',
        '1001001002',
        'CC',
        'Operario Impresión Litografía',
        'operario.impresion@indicolors.com',
        '3001001002',
        'Antioquia',
        'Medellín',
        'Planta Litografía, Medellín',
        '$2a$10$WGGWmu2DjL2BnBljv70Gzu7TShx6GHdIu/.ivtnSklhahp1zWJwQi',
        CURRENT_DATE,
        TRUE,
        1,
        FALSE,
        0
    ),
    (
        'operator-seed-003',
        'company-seed-001',
        '1001001003',
        'CC',
        'Operario Acabados Litografía',
        'operario.acabados@indicolors.com',
        '3001001003',
        'Antioquia',
        'Medellín',
        'Planta Litografía, Medellín',
        '$2a$10$WGGWmu2DjL2BnBljv70Gzu7TShx6GHdIu/.ivtnSklhahp1zWJwQi',
        CURRENT_DATE,
        TRUE,
        1,
        FALSE,
        0
    )
ON CONFLICT (user_id) DO NOTHING;

-- Rol Operador (b1ffbc99-9c0b-4ef8-bb6d-6bb9bd380a22)
INSERT INTO indicolors.user_roles (user_id, role_id)
VALUES
    ('operator-seed-001', 'b1ffbc99-9c0b-4ef8-bb6d-6bb9bd380a22'::uuid),
    ('operator-seed-002', 'b1ffbc99-9c0b-4ef8-bb6d-6bb9bd380a22'::uuid),
    ('operator-seed-003', 'b1ffbc99-9c0b-4ef8-bb6d-6bb9bd380a22'::uuid)
ON CONFLICT DO NOTHING;


-- ---------------------------------------------------------------------------
-- Clientes
-- ---------------------------------------------------------------------------
INSERT INTO indicolors.clients (
    client_id,
    company_id,
    name,
    document_type,
    identification,
    department,
    city,
    address,
    phone,
    email,
    contact_person,
    state,
    creation_date
) VALUES
    (
        'client-seed-001',
        'company-seed-001',
        'Editorial Horizonte Ltda.',
        'NIT',
        '800222001-1',
        'Antioquia',
        'Medellín',
        'Calle 10 # 43-20',
        '6042220001',
        'compras@editorialhorizonte.com',
        'María López',
        TRUE,
        CURRENT_DATE
    ),
    (
        'client-seed-002',
        'company-seed-001',
        'Agencia Visual Branding',
        'NIT',
        '800222002-2',
        'Antioquia',
        'Envigado',
        'Carrera 43A # 1 Sur-50',
        '6042220002',
        'produccion@visualbranding.com',
        'Andrés Mejía',
        TRUE,
        CURRENT_DATE
    ),
    (
        'client-seed-003',
        'company-seed-001',
        'Empaques Industriales del Norte',
        'NIT',
        '800222003-3',
        'Atlántico',
        'Barranquilla',
        'Calle 30 # 8-15',
        '6052220003',
        'ordenes@empaquesnorte.com',
        'Sofía Castro',
        TRUE,
        CURRENT_DATE
    )
ON CONFLICT (client_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Vendedores
-- ---------------------------------------------------------------------------
INSERT INTO indicolors.sellers (
    seller_id,
    company_id,
    full_name,
    document_type,
    identification,
    email,
    phone,
    department,
    city,
    address,
    state,
    creation_date
) VALUES
    (
        'seller-seed-001',
        'company-seed-001',
        'Juan Camilo Restrepo',
        'CC',
        '1033001001',
        'juan.restrepo@indicolors.com',
        '3003001001',
        'Antioquia',
        'Medellín',
        'Medellín, Colombia',
        TRUE,
        CURRENT_DATE
    ),
    (
        'seller-seed-002',
        'company-seed-001',
        'Valentina Hoyos',
        'CC',
        '1033001002',
        'valentina.hoyos@indicolors.com',
        '3003001002',
        'Antioquia',
        'Medellín',
        'Medellín, Colombia',
        TRUE,
        CURRENT_DATE
    ),
    (
        'seller-seed-003',
        'company-seed-001',
        'Diego Alejandro Quintero',
        'CC',
        '1033001003',
        'diego.quintero@indicolors.com',
        '3003001003',
        'Antioquia',
        'Bello',
        'Bello, Colombia',
        TRUE,
        CURRENT_DATE
    )
ON CONFLICT (seller_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Cuentas bancarias
-- ---------------------------------------------------------------------------
INSERT INTO indicolors.bank_accounts (
    account_id,
    company_id,
    bank_name,
    account_type,
    account_number,
    holder_name,
    holder_nit,
    include_in_pdf,
    is_primary,
    state,
    creation_date
) VALUES
    (
        'account-seed-001',
        'company-seed-001',
        'Bancolombia',
        'Corriente',
        '12345678901',
        'InkCore S.A.S.',
        '900123456-7',
        TRUE,
        TRUE,
        TRUE,
        CURRENT_DATE
    ),
    (
        'account-seed-002',
        'company-seed-001',
        'Davivienda',
        'Ahorros',
        '98765432100',
        'InkCore S.A.S.',
        '900123456-7',
        TRUE,
        FALSE,
        TRUE,
        CURRENT_DATE
    )
ON CONFLICT (account_id) DO NOTHING;



