BEGIN;

-- Los despliegues gratuitos sin volumen persistente conservan documentos y
-- fotografías dentro de PostgreSQL. Las rutas existentes siguen siendo
-- válidas para no romper instalaciones que ya utilizan almacenamiento local.

ALTER TABLE soracare.documentos
    ADD COLUMN IF NOT EXISTS contenido_archivo BYTEA;

ALTER TABLE soracare.documentos
    ALTER COLUMN ruta_almacenamiento DROP NOT NULL;

ALTER TABLE soracare.documentos
    DROP CONSTRAINT IF EXISTS ck_documentos_ubicacion_archivo;

ALTER TABLE soracare.documentos
    ADD CONSTRAINT ck_documentos_ubicacion_archivo
    CHECK (
        (contenido_archivo IS NOT NULL AND ruta_almacenamiento IS NULL)
        OR
        (contenido_archivo IS NULL
            AND ruta_almacenamiento IS NOT NULL
            AND BTRIM(ruta_almacenamiento) <> '')
    );

ALTER TABLE soracare.documentos
    DROP CONSTRAINT IF EXISTS ck_documentos_contenido_tamano;

ALTER TABLE soracare.documentos
    ADD CONSTRAINT ck_documentos_contenido_tamano
    CHECK (
        contenido_archivo IS NULL
        OR (
            OCTET_LENGTH(contenido_archivo) BETWEEN 1 AND 10485760
            AND tamano_bytes = OCTET_LENGTH(contenido_archivo)
        )
    );

ALTER TABLE soracare.perfiles_visuales
    ADD COLUMN IF NOT EXISTS contenido_foto BYTEA;

ALTER TABLE soracare.perfiles_visuales
    DROP CONSTRAINT IF EXISTS ck_perfiles_visuales_foto_completa;

ALTER TABLE soracare.perfiles_visuales
    ADD CONSTRAINT ck_perfiles_visuales_foto_completa
    CHECK (
        (
            ruta_foto IS NULL AND contenido_foto IS NULL
            AND tipo_mime IS NULL AND tamano_bytes IS NULL
            AND hash_sha256 IS NULL
        )
        OR
        (
            ruta_foto IS NOT NULL AND BTRIM(ruta_foto) <> ''
            AND contenido_foto IS NULL
            AND tipo_mime IN ('image/jpeg', 'image/png')
            AND tamano_bytes BETWEEN 1 AND 5242880
            AND hash_sha256 ~ '^[a-f0-9]{64}$'
        )
        OR
        (
            ruta_foto IS NULL AND contenido_foto IS NOT NULL
            AND tipo_mime IN ('image/jpeg', 'image/png')
            AND tamano_bytes = OCTET_LENGTH(contenido_foto)
            AND tamano_bytes BETWEEN 1 AND 5242880
            AND hash_sha256 ~ '^[a-f0-9]{64}$'
        )
    );

COMMENT ON COLUMN soracare.documentos.contenido_archivo IS
    'Contenido binario privado para despliegues sin volumen persistente';

COMMENT ON COLUMN soracare.perfiles_visuales.contenido_foto IS
    'Fotografía privada almacenada en PostgreSQL cuando no existe volumen persistente';

COMMIT;
