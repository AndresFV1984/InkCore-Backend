-- Historial de estimaciones de consumo de tinta (metadatos; el archivo no se almacena).

CREATE TABLE IF NOT EXISTS indicolors.ink_estimate_history (
    ink_estimate_history_id VARCHAR(64)    NOT NULL,
    original_file_name      VARCHAR(255)   NOT NULL,
    original_size_bytes     BIGINT         NOT NULL,
    mime_type               VARCHAR(120),
    width_cm                NUMERIC(12, 4) NOT NULL,
    height_cm               NUMERIC(12, 4) NOT NULL,
    sheet_count             INTEGER        NOT NULL,
    dpi                     INTEGER        NOT NULL,
    grams_per_cm2           NUMERIC(16, 10) NOT NULL,
    process_grams_order     NUMERIC(18, 6) NOT NULL,
    spot_grams_order        NUMERIC(18, 6) NOT NULL,
    total_grams_order       NUMERIC(18, 6) NOT NULL,
    spot_count              INTEGER        NOT NULL DEFAULT 0,
    icc_profile_used        VARCHAR(120),
    estimate_date           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    duration_ms             BIGINT         NOT NULL,
    user_id                 VARCHAR(64),
    CONSTRAINT ink_estimate_history_pkey PRIMARY KEY (ink_estimate_history_id),
    CONSTRAINT ink_estimate_history_sizes_check
        CHECK (original_size_bytes >= 0),
    CONSTRAINT ink_estimate_history_dims_check
        CHECK (width_cm > 0 AND height_cm > 0),
    CONSTRAINT ink_estimate_history_sheets_check
        CHECK (sheet_count > 0),
    CONSTRAINT ink_estimate_history_dpi_check
        CHECK (dpi > 0),
    CONSTRAINT ink_estimate_history_duration_check
        CHECK (duration_ms >= 0)
);

CREATE INDEX IF NOT EXISTS idx_ink_estimate_history_user_id
    ON indicolors.ink_estimate_history (user_id);
CREATE INDEX IF NOT EXISTS idx_ink_estimate_history_estimate_date
    ON indicolors.ink_estimate_history (estimate_date DESC);

COMMENT ON TABLE indicolors.ink_estimate_history IS 'Metadatos de estimaciones de consumo de tinta (el binario no se persiste)';
