-- Máquinas de la litografía de demo (company-seed-001), una por fase.
-- cost_per_hour lo calcula la columna generada. El trigger de machines
-- deja el primer snapshot en machine_cost_history.
-- La offset de impresión usa el id de los ejemplos de Swagger.

INSERT INTO indicolors.machines (
    machine_id,
    company_id,
    name,
    machine_type,
    manufacturer,
    model,
    purchase_cost,
    useful_life_years,
    annual_maintenance_cost,
    monthly_operator_cost,
    energy_cost_per_hour,
    productive_hours_per_year,
    state,
    creation_date
) VALUES
    (
        'machine-seed-preprensa-001',
        'company-seed-001',
        'CTP Kodak Trendsetter',
        'preprensa',
        'Kodak',
        'Trendsetter 800',
        160000000.00,
        10.00,
        4000000.00,
        2000000.00,
        5000.00,
        1600.00,
        TRUE,
        CURRENT_DATE
    ),
    (
        'machine-seed-corte-001',
        'company-seed-001',
        'Guillotina Polar 115',
        'corte-papel',
        'Polar',
        '115',
        80000000.00,
        10.00,
        2000000.00,
        1800000.00,
        2500.00,
        1600.00,
        TRUE,
        CURRENT_DATE
    ),
    (
        '814ad646-c4fe-42fa-9f13-4a44823e6bee',
        'company-seed-001',
        'Offset Heidelberg 4 colores',
        'impresion',
        'Heidelberg',
        'SM 74',
        250000000.00,
        10.00,
        8000000.00,
        2500000.00,
        15000.00,
        1600.00,
        TRUE,
        CURRENT_DATE
    ),
    (
        'machine-seed-terminados-001',
        'company-seed-001',
        'Plegadora Stahlfolder',
        'terminados',
        'Heidelberg',
        'Stahlfolder Ti 52',
        90000000.00,
        10.00,
        2400000.00,
        1600000.00,
        3000.00,
        1600.00,
        TRUE,
        CURRENT_DATE
    ),
    (
        'machine-seed-acabados-001',
        'company-seed-001',
        'Laminadora Autobond',
        'acabados',
        'Autobond',
        'Mini 74',
        70000000.00,
        10.00,
        1800000.00,
        1500000.00,
        4000.00,
        1600.00,
        TRUE,
        CURRENT_DATE
    )
ON CONFLICT (machine_id) DO NOTHING;
