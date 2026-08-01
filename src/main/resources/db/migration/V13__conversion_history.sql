-- Historial de conversiones RGB → CMYK (metadatos; el archivo convertido no se almacena).

CREATE TABLE IF NOT EXISTS indicolors.conversion_history (
    conversion_history_id VARCHAR(64)    NOT NULL,
    original_file_name    VARCHAR(255)   NOT NULL,
    original_size_bytes   BIGINT         NOT NULL,
    final_size_bytes      BIGINT         NOT NULL,
    rendering_intent      VARCHAR(40)    NOT NULL,
    icc_profile_used      VARCHAR(120),
    conversion_date       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    duration_ms           BIGINT         NOT NULL,
    user_id               VARCHAR(64),
    mime_type             VARCHAR(120),
    CONSTRAINT conversion_history_pkey PRIMARY KEY (conversion_history_id),
    CONSTRAINT conversion_history_sizes_check
        CHECK (original_size_bytes >= 0 AND final_size_bytes >= 0),
    CONSTRAINT conversion_history_duration_check
        CHECK (duration_ms >= 0),
    CONSTRAINT conversion_history_intent_check
        CHECK (rendering_intent IN ('PERCEPTUAL', 'RELATIVE_COLORIMETRIC'))
);

CREATE INDEX IF NOT EXISTS idx_conversion_history_user_id ON indicolors.conversion_history (user_id);
CREATE INDEX IF NOT EXISTS idx_conversion_history_conversion_date ON indicolors.conversion_history (conversion_date DESC);
CREATE INDEX IF NOT EXISTS idx_conversion_history_intent ON indicolors.conversion_history (rendering_intent);

COMMENT ON TABLE indicolors.conversion_history IS 'Metadatos de conversiones de color RGB→CMYK (el binario no se persiste)';
COMMENT ON COLUMN indicolors.conversion_history.conversion_history_id IS 'Identificador único de la conversión';
COMMENT ON COLUMN indicolors.conversion_history.original_file_name IS 'Nombre del archivo original subido';
COMMENT ON COLUMN indicolors.conversion_history.original_size_bytes IS 'Tamaño en bytes del archivo de entrada';
COMMENT ON COLUMN indicolors.conversion_history.final_size_bytes IS 'Tamaño en bytes del archivo CMYK (suele ser mayor por 4 canales)';
COMMENT ON COLUMN indicolors.conversion_history.rendering_intent IS 'Intent ICC: PERCEPTUAL o RELATIVE_COLORIMETRIC';
COMMENT ON COLUMN indicolors.conversion_history.icc_profile_used IS 'Nombre del perfil ICC de destino usado';
COMMENT ON COLUMN indicolors.conversion_history.conversion_date IS 'Fecha/hora UTC de la conversión';
COMMENT ON COLUMN indicolors.conversion_history.duration_ms IS 'Duración del procesamiento en milisegundos';
COMMENT ON COLUMN indicolors.conversion_history.user_id IS 'Usuario autenticado (subject del JWT), si aplica';
COMMENT ON COLUMN indicolors.conversion_history.mime_type IS 'MIME type declarado en la petición';
