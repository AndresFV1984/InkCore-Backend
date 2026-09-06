-- Datos semilla de litografía (company-seed-001) para desarrollo / demos.
-- 3 usuarios Operador, 3 proveedores, 3 clientes, 3 vendedores,
-- 2 cuentas bancarias, 2 despieces, 2 tipos de papel (con proveedores y despieces asociados).
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
-- Proveedores
-- ---------------------------------------------------------------------------
INSERT INTO indicolors.suppliers (
    supplier_id,
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
        'supplier-seed-001',
        'company-seed-001',
        'Papeles del Oriente S.A.S.',
        'NIT',
        '900111001-1',
        'Antioquia',
        'Medellín',
        'Calle 30 # 55-10, Zona Industrial',
        '6041110001',
        'ventas@papelesoriente.com',
        'Laura Gómez',
        TRUE,
        CURRENT_DATE
    ),
    (
        'supplier-seed-002',
        'company-seed-001',
        'Insumos Gráficos Andina',
        'NIT',
        '900111002-2',
        'Antioquia',
        'Itagüí',
        'Carrera 52 # 40-20',
        '6041110002',
        'contacto@insumosandina.com',
        'Carlos Ruiz',
        TRUE,
        CURRENT_DATE
    ),
    (
        'supplier-seed-003',
        'company-seed-001',
        'Tintas y Químicos Valle',
        'NIT',
        '900111003-3',
        'Valle del Cauca',
        'Cali',
        'Av. 3N # 15-45',
        '6021110003',
        'comercial@tintasvalle.com',
        'Ana Pérez',
        TRUE,
        CURRENT_DATE
    )
ON CONFLICT (supplier_id) DO NOTHING;

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

-- ---------------------------------------------------------------------------
-- Despieces
-- ---------------------------------------------------------------------------
INSERT INTO indicolors.cut_layouts (
    cut_layout_id,
    company_id,
    name,
    width,
    height,
    unit,
    pieces_per_sheet,
    state,
    creation_date
) VALUES
    (
        'cut-layout-seed-001',
        'company-seed-001',
        'Etiqueta',
        10.00,
        5.00,
        'cm',
        24,
        TRUE,
        CURRENT_DATE
    ),
    (
        'cut-layout-seed-002',
        'company-seed-001',
        'Flyer A5',
        14.80,
        21.00,
        'cm',
        8,
        TRUE,
        CURRENT_DATE
    )
ON CONFLICT (cut_layout_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Tipos de papel (con proveedores y despieces asociados)
-- ---------------------------------------------------------------------------
INSERT INTO indicolors.paper_types (
    paper_type_id,
    company_id,
    name,
    width,
    height,
    unit,
    is_coated,
    state,
    creation_date
) VALUES
    (
        'paper-type-seed-001',
        'company-seed-001',
        'Bond 75g',
        70.00,
        100.00,
        'cm',
        FALSE,
        TRUE,
        CURRENT_DATE
    ),
    (
        'paper-type-seed-002',
        'company-seed-001',
        'Couché 150g',
        72.00,
        102.00,
        'cm',
        TRUE,
        TRUE,
        CURRENT_DATE
    )
ON CONFLICT (paper_type_id) DO NOTHING;

INSERT INTO indicolors.paper_type_cut_layouts (
    paper_type_id,
    cut_layout_id,
    cut_value
) VALUES
    ('paper-type-seed-001', 'cut-layout-seed-001', 200.00),
    ('paper-type-seed-001', 'cut-layout-seed-002', 180.00),
    ('paper-type-seed-002', 'cut-layout-seed-001', 220.00),
    ('paper-type-seed-002', 'cut-layout-seed-002', 195.00)
ON CONFLICT (paper_type_id, cut_layout_id) DO NOTHING;

INSERT INTO indicolors.paper_type_suppliers (
    paper_type_id,
    supplier_id,
    sheet_value,
    package_unit
) VALUES
    ('paper-type-seed-001', 'supplier-seed-001', 1500.00, 500),
    ('paper-type-seed-001', 'supplier-seed-002', 1480.00, 500),
    ('paper-type-seed-002', 'supplier-seed-001', 3200.00, 250),
    ('paper-type-seed-002', 'supplier-seed-002', 3100.00, 250)
ON CONFLICT (paper_type_id, supplier_id) DO NOTHING;
