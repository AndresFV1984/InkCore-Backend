-- Tabla bank_accounts (formulario "Nueva cuenta bancaria") — un archivo por tabla, DDL completo.

CREATE TABLE IF NOT EXISTS indicolors.bank_accounts (
    account_id     VARCHAR(64)  NOT NULL DEFAULT gen_random_uuid()::text,
    company_id     VARCHAR(64)  NOT NULL,
    bank_name      VARCHAR(150) NOT NULL,
    account_type   VARCHAR(30)  NOT NULL,
    account_number VARCHAR(50)  NOT NULL,
    holder_name    VARCHAR(200) NOT NULL,
    holder_nit     VARCHAR(32),
    include_in_pdf BOOLEAN      NOT NULL DEFAULT TRUE,
    is_primary     BOOLEAN      NOT NULL DEFAULT FALSE,
    state          BOOLEAN      NOT NULL DEFAULT TRUE,
    creation_date  DATE         NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT bank_accounts_pkey PRIMARY KEY (account_id),
    CONSTRAINT bank_accounts_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT bank_accounts_number_company_unique
        UNIQUE (company_id, account_number),
    CONSTRAINT bank_accounts_type_check
        CHECK (account_type IN ('Ahorros', 'Corriente'))
);

CREATE INDEX IF NOT EXISTS idx_bank_accounts_company_id ON indicolors.bank_accounts (company_id);
CREATE INDEX IF NOT EXISTS idx_bank_accounts_state ON indicolors.bank_accounts (state);
CREATE INDEX IF NOT EXISTS idx_bank_accounts_bank_name ON indicolors.bank_accounts (bank_name);
CREATE INDEX IF NOT EXISTS idx_bank_accounts_company_state ON indicolors.bank_accounts (company_id, state);

-- Como máximo una cuenta principal por compañía (columna "Principal" del listado).
CREATE UNIQUE INDEX IF NOT EXISTS idx_bank_accounts_one_primary_per_company
    ON indicolors.bank_accounts (company_id)
    WHERE is_primary = TRUE;

COMMENT ON TABLE indicolors.bank_accounts IS 'Cuentas bancarias de la compañía, usadas para cobros y generación de PDF de costeo al cliente';
COMMENT ON COLUMN indicolors.bank_accounts.account_id IS 'Identificador único de la cuenta bancaria';
COMMENT ON COLUMN indicolors.bank_accounts.company_id IS 'Identificador de la empresa dueña de la cuenta';
COMMENT ON COLUMN indicolors.bank_accounts.bank_name IS 'Nombre del banco (ej. Bancolombia)';
COMMENT ON COLUMN indicolors.bank_accounts.account_type IS 'Tipo de cuenta: Ahorros o Corriente';
COMMENT ON COLUMN indicolors.bank_accounts.account_number IS 'Número de la cuenta bancaria';
COMMENT ON COLUMN indicolors.bank_accounts.holder_name IS 'Nombre o razón social del titular de la cuenta';
COMMENT ON COLUMN indicolors.bank_accounts.holder_nit IS 'NIT del titular de la cuenta, si aplica';
COMMENT ON COLUMN indicolors.bank_accounts.include_in_pdf IS 'True=Incluir esta cuenta en el PDF de costeo al cliente (campo "Uso en documentos")';
COMMENT ON COLUMN indicolors.bank_accounts.is_primary IS 'True=Cuenta principal de la compañía (solo una por compañía)';
COMMENT ON COLUMN indicolors.bank_accounts.state IS 'True=Activa, False=Inactiva';
COMMENT ON COLUMN indicolors.bank_accounts.creation_date IS 'Fecha de registro de la cuenta en el sistema';
