BEGIN;

-- La numeracion tributaria es independiente de la numeracion FI interna.
-- Cada ambiente mantiene su propia secuencia para evitar que las pruebas
-- consuman numeros del eventual timbrado de produccion.
CREATE TABLE soracare.numeraciones_sifen (
    ambiente VARCHAR(12) NOT NULL,
    tipo_documento SMALLINT NOT NULL DEFAULT 1,
    establecimiento CHAR(3) NOT NULL,
    punto_expedicion CHAR(3) NOT NULL,
    serie VARCHAR(2) NOT NULL DEFAULT '',
    siguiente_numero INTEGER NOT NULL DEFAULT 1,
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_numeraciones_sifen PRIMARY KEY (
        ambiente, tipo_documento, establecimiento, punto_expedicion, serie
    ),
    CONSTRAINT chk_numeraciones_sifen_ambiente
        CHECK (ambiente IN ('PRUEBA', 'PRODUCCION')),
    CONSTRAINT chk_numeraciones_sifen_tipo_documento
        CHECK (tipo_documento BETWEEN 1 AND 8),
    CONSTRAINT chk_numeraciones_sifen_establecimiento
        CHECK (establecimiento ~ '^[0-9]{3}$'),
    CONSTRAINT chk_numeraciones_sifen_punto
        CHECK (punto_expedicion ~ '^[0-9]{3}$'),
    CONSTRAINT chk_numeraciones_sifen_serie
        CHECK (serie = '' OR serie ~ '^[A-Z0-9]{2}$'),
    CONSTRAINT chk_numeraciones_sifen_siguiente
        CHECK (siguiente_numero BETWEEN 1 AND 10000000)
);

CREATE SEQUENCE soracare.sifen_identificador_envio_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    MAXVALUE 999999999999999
    NO CYCLE;

ALTER TABLE soracare.solicitudes_sifen
    ADD COLUMN establecimiento CHAR(3),
    ADD COLUMN punto_expedicion CHAR(3),
    ADD COLUMN serie VARCHAR(2) NOT NULL DEFAULT '',
    ADD COLUMN numero_documento CHAR(7),
    ADD COLUMN codigo_seguridad CHAR(9),
    ADD COLUMN dv_cdc SMALLINT,
    ADD COLUMN afectacion_iva SMALLINT,
    ADD COLUMN tasa_iva SMALLINT,
    ADD COLUMN total_operacion NUMERIC(14, 2),
    ADD COLUMN total_iva NUMERIC(14, 2),
    ADD COLUMN xml_sin_firma BYTEA,
    ADD COLUMN xml_firmado BYTEA,
    ADD COLUMN url_qr TEXT,
    ADD COLUMN xsd_validado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN firmado_en TIMESTAMP WITH TIME ZONE;

ALTER TABLE soracare.solicitudes_sifen
    ADD COLUMN identificador_envio BIGINT,
    ADD COLUMN codigo_http SMALLINT,
    ADD COLUMN respuesta_xml BYTEA;

ALTER TABLE soracare.solicitudes_sifen
    DROP CONSTRAINT chk_solicitudes_sifen_estado,
    ADD CONSTRAINT chk_solicitudes_sifen_estado
        CHECK (estado IN (
            'PREPARACION', 'PENDIENTE_FIRMA', 'FIRMADO', 'VALIDADO',
            'ENVIADO', 'APROBADO', 'RECHAZADO', 'ERROR', 'CANCELADO'
        )),
    ADD CONSTRAINT chk_solicitudes_sifen_numero
        CHECK (numero_documento IS NULL OR numero_documento ~ '^[0-9]{7}$'),
    ADD CONSTRAINT chk_solicitudes_sifen_establecimiento
        CHECK (establecimiento IS NULL OR establecimiento ~ '^[0-9]{3}$'),
    ADD CONSTRAINT chk_solicitudes_sifen_punto
        CHECK (punto_expedicion IS NULL OR punto_expedicion ~ '^[0-9]{3}$'),
    ADD CONSTRAINT chk_solicitudes_sifen_numeracion_completa
        CHECK (
            (establecimiento IS NULL AND punto_expedicion IS NULL AND numero_documento IS NULL)
            OR (establecimiento IS NOT NULL AND punto_expedicion IS NOT NULL AND numero_documento IS NOT NULL)
        ),
    ADD CONSTRAINT chk_solicitudes_sifen_serie
        CHECK (serie = '' OR serie ~ '^[A-Z0-9]{2}$'),
    ADD CONSTRAINT chk_solicitudes_sifen_codigo_seguridad
        CHECK (
            codigo_seguridad IS NULL
            OR (codigo_seguridad ~ '^[0-9]{9}$' AND codigo_seguridad <> '000000000')
        ),
    ADD CONSTRAINT chk_solicitudes_sifen_dv
        CHECK (dv_cdc IS NULL OR dv_cdc BETWEEN 0 AND 9),
    ADD CONSTRAINT chk_solicitudes_sifen_afectacion
        CHECK (afectacion_iva IS NULL OR afectacion_iva BETWEEN 1 AND 4),
    ADD CONSTRAINT chk_solicitudes_sifen_tasa
        CHECK (tasa_iva IS NULL OR tasa_iva IN (0, 5, 10)),
    ADD CONSTRAINT chk_solicitudes_sifen_totales
        CHECK (
            (total_operacion IS NULL AND total_iva IS NULL)
            OR (total_operacion > 0 AND total_iva >= 0 AND total_iva <= total_operacion)
        ),
    ADD CONSTRAINT chk_solicitudes_sifen_xml_validado
        CHECK (
            NOT xsd_validado
            OR (
                estado IN (
                    'VALIDADO', 'ENVIADO', 'APROBADO', 'RECHAZADO',
                    'ERROR', 'CANCELADO'
                )
                AND xml_firmado IS NOT NULL
                AND cdc IS NOT NULL
            )
        ),
    ADD CONSTRAINT chk_solicitudes_sifen_identificador_envio
        CHECK (identificador_envio IS NULL OR identificador_envio > 0),
    ADD CONSTRAINT chk_solicitudes_sifen_codigo_http
        CHECK (codigo_http IS NULL OR codigo_http BETWEEN 100 AND 599);

CREATE UNIQUE INDEX uq_solicitudes_sifen_numero_fiscal
    ON soracare.solicitudes_sifen (
        ambiente, establecimiento, punto_expedicion, serie, numero_documento
    )
    WHERE numero_documento IS NOT NULL;

COMMENT ON TABLE soracare.numeraciones_sifen IS
    'Asignacion transaccional de numeros de DE, separada por ambiente y punto de expedicion';

COMMENT ON COLUMN soracare.solicitudes_sifen.xml_firmado IS
    'XML firmado y validado localmente; su presencia no implica aprobacion del SIFEN';

ALTER TABLE soracare.numeraciones_sifen OWNER TO soracare_app;
ALTER SEQUENCE soracare.sifen_identificador_envio_seq OWNER TO soracare_app;

COMMIT;
