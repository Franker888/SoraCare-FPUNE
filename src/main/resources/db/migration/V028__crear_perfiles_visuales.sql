BEGIN;

CREATE TABLE IF NOT EXISTS soracare.perfiles_visuales (
    id_persona BIGINT PRIMARY KEY,
    avatar_codigo VARCHAR(20) NOT NULL DEFAULT 'AZUL',
    ruta_foto VARCHAR(500),
    tipo_mime VARCHAR(50),
    tamano_bytes BIGINT,
    hash_sha256 CHAR(64),
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_perfiles_visuales_personas
        FOREIGN KEY (id_persona)
        REFERENCES soracare.personas (id_persona)
        ON UPDATE RESTRICT
        ON DELETE CASCADE,

    CONSTRAINT ck_perfiles_visuales_avatar
        CHECK (avatar_codigo IN (
            'AZUL', 'TURQUESA', 'VIOLETA', 'CORAL', 'VERDE', 'DORADO'
        )),

    CONSTRAINT ck_perfiles_visuales_foto_completa
        CHECK (
            (ruta_foto IS NULL AND tipo_mime IS NULL
                AND tamano_bytes IS NULL AND hash_sha256 IS NULL)
            OR
            (ruta_foto IS NOT NULL AND BTRIM(ruta_foto) <> ''
                AND tipo_mime IS NOT NULL AND tipo_mime IN ('image/jpeg', 'image/png')
                AND tamano_bytes IS NOT NULL AND tamano_bytes BETWEEN 1 AND 5242880
                AND hash_sha256 IS NOT NULL AND hash_sha256 ~ '^[a-f0-9]{64}$')
        )
);

CREATE INDEX IF NOT EXISTS ix_perfiles_visuales_actualizacion
    ON soracare.perfiles_visuales (fecha_actualizacion DESC);

COMMENT ON TABLE soracare.perfiles_visuales IS
    'Avatar o fotografía privada utilizada en los portales personales de SoraCare';

COMMENT ON COLUMN soracare.perfiles_visuales.ruta_foto IS
    'Ruta relativa dentro del almacén privado de fotografías de perfil';

ALTER TABLE soracare.perfiles_visuales OWNER TO soracare_app;

COMMIT;
