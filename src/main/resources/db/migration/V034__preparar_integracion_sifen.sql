BEGIN;

-- Registro técnico para la futura transmisión a e-Kuatia/SIFEN. Esta tabla
-- no convierte la factura interna en un documento tributario ni guarda secretos.
CREATE TABLE soracare.solicitudes_sifen (
    id_solicitud BIGINT GENERATED ALWAYS AS IDENTITY,
    id_factura_interna BIGINT NOT NULL,
    ambiente VARCHAR(12) NOT NULL,
    estado VARCHAR(24) NOT NULL DEFAULT 'PREPARACION',
    cdc CHAR(44),
    numero_lote BIGINT,
    protocolo_autorizacion VARCHAR(80),
    codigo_respuesta VARCHAR(20),
    mensaje_respuesta VARCHAR(1000),
    hash_xml_sha256 CHAR(64),
    intentos SMALLINT NOT NULL DEFAULT 0,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enviado_en TIMESTAMP WITH TIME ZONE,
    respondido_en TIMESTAMP WITH TIME ZONE,
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_solicitudes_sifen
        PRIMARY KEY (id_solicitud),

    CONSTRAINT uq_solicitudes_sifen_factura
        UNIQUE (id_factura_interna),

    CONSTRAINT fk_solicitudes_sifen_factura
        FOREIGN KEY (id_factura_interna)
            REFERENCES soracare.facturas_internas (id_factura)
            ON DELETE RESTRICT,

    CONSTRAINT chk_solicitudes_sifen_ambiente
        CHECK (ambiente IN ('PRUEBA', 'PRODUCCION')),

    CONSTRAINT chk_solicitudes_sifen_estado
        CHECK (estado IN (
            'PREPARACION', 'PENDIENTE_FIRMA', 'FIRMADO', 'ENVIADO',
            'APROBADO', 'RECHAZADO', 'ERROR', 'CANCELADO'
        )),

    CONSTRAINT chk_solicitudes_sifen_cdc
        CHECK (cdc IS NULL OR cdc ~ '^[0-9]{44}$'),

    CONSTRAINT chk_solicitudes_sifen_hash
        CHECK (hash_xml_sha256 IS NULL OR hash_xml_sha256 ~ '^[A-Fa-f0-9]{64}$'),

    CONSTRAINT chk_solicitudes_sifen_intentos
        CHECK (intentos >= 0)
);

CREATE INDEX idx_solicitudes_sifen_estado
    ON soracare.solicitudes_sifen (ambiente, estado, actualizado_en DESC);

COMMENT ON TABLE soracare.solicitudes_sifen IS
    'Trazabilidad técnica de la futura integración e-Kuatia/SIFEN; no implica aprobación tributaria';

COMMENT ON COLUMN soracare.solicitudes_sifen.cdc IS
    'CDC informado por el flujo fiscal; nunca debe confundirse con el número FI interno';

ALTER TABLE soracare.solicitudes_sifen OWNER TO soracare_app;

COMMIT;
