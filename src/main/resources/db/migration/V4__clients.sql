-- Clientes del directorio comercial
CREATE TABLE indicolors.clients (
    id          VARCHAR(64) NOT NULL,
    name        VARCHAR(200) NOT NULL,
    nit         VARCHAR(50),
    phone       VARCHAR(50),
    city        VARCHAR(100),
    address     VARCHAR(300),
    email       VARCHAR(150),
    contact     VARCHAR(150),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_clients_nit
    ON indicolors.clients (LOWER(nit))
    WHERE nit IS NOT NULL AND TRIM(nit) <> '';

CREATE INDEX ix_clients_name ON indicolors.clients (name);
CREATE INDEX ix_clients_active ON indicolors.clients (active);

-- Semilla opcional (ejemplo)
INSERT INTO indicolors.clients (id, name, nit, phone, city, address, email, contact, active)
VALUES (
    '11111111-1111-1111-1111-111111111111'::uuid,
    'Cliente Demo IndiColors',
    '900999888-1',
    '3009876543',
    'Medellín',
    'Carrera demo 10',
    'demo@cliente.com',
    'Contacto demo',
    TRUE
)
ON CONFLICT (id) DO NOTHING;
