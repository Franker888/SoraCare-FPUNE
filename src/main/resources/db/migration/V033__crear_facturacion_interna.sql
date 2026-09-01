BEGIN;

-- La numeracion interna se mantiene separada de los comprobantes de pago y
-- de la futura numeracion tributaria de e-Kuatia/SIFEN.
CREATE SEQUENCE soracare.facturas_internas_numero_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE soracare.facturas_internas (
    id_factura BIGINT GENERATED ALWAYS AS IDENTITY,
    id_pago BIGINT NOT NULL,
    id_usuario_emisor BIGINT NOT NULL,

    numero_factura VARCHAR(30) NOT NULL DEFAULT (
        'FI-' || TO_CHAR(CURRENT_DATE, 'YYYY') || '-'
        || LPAD(NEXTVAL('soracare.facturas_internas_numero_seq')::TEXT, 8, '0')
    ),
    estado VARCHAR(20) NOT NULL DEFAULT 'EMITIDA',

    tipo_documento VARCHAR(15) NOT NULL,
    numero_documento VARCHAR(30) NOT NULL,
    nombre_razon_social VARCHAR(160) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    correo VARCHAR(255) NOT NULL,

    subtotal NUMERIC(14, 2) NOT NULL,
    total NUMERIC(14, 2) NOT NULL,
    moneda CHAR(3) NOT NULL DEFAULT 'PYG',
    descripcion VARCHAR(255) NOT NULL,

    validez_tributaria BOOLEAN NOT NULL DEFAULT FALSE,
    nombre_archivo VARCHAR(255),
    contenido_pdf BYTEA,
    hash_sha256 CHAR(64),

    estado_envio_email VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_emision TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_envio_email TIMESTAMP WITH TIME ZONE,
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_facturas_internas
        PRIMARY KEY (id_factura),

    CONSTRAINT uq_facturas_internas_pago
        UNIQUE (id_pago),

    CONSTRAINT uq_facturas_internas_numero
        UNIQUE (numero_factura),

    CONSTRAINT fk_facturas_internas_pago
        FOREIGN KEY (id_pago)
            REFERENCES soracare.pagos (id_pago)
            ON DELETE RESTRICT,

    CONSTRAINT fk_facturas_internas_usuario
        FOREIGN KEY (id_usuario_emisor)
            REFERENCES soracare.usuarios (id_usuario)
            ON DELETE RESTRICT,

    CONSTRAINT chk_facturas_internas_estado
        CHECK (estado IN ('EMITIDA', 'ANULADA')),

    CONSTRAINT chk_facturas_internas_documento
        CHECK (
            tipo_documento IN ('CI', 'RUC', 'PASAPORTE')
            AND BTRIM(numero_documento) <> ''
            AND BTRIM(nombre_razon_social) <> ''
            AND BTRIM(direccion) <> ''
            AND BTRIM(correo) <> ''
        ),

    CONSTRAINT chk_facturas_internas_importes
        CHECK (subtotal > 0 AND total > 0 AND total = subtotal),

    CONSTRAINT chk_facturas_internas_moneda
        CHECK (moneda ~ '^[A-Z]{3}$'),

    CONSTRAINT chk_facturas_internas_sin_validez_tributaria
        CHECK (validez_tributaria = FALSE),

    CONSTRAINT chk_facturas_internas_hash
        CHECK (hash_sha256 IS NULL OR hash_sha256 ~ '^[A-Fa-f0-9]{64}$'),

    CONSTRAINT chk_facturas_internas_estado_email
        CHECK (estado_envio_email IN ('PENDIENTE', 'ENVIADO', 'FALLIDO', 'NO_CONFIGURADO'))
);

CREATE TABLE soracare.facturas_internas_detalles (
    id_detalle BIGINT GENERATED ALWAYS AS IDENTITY,
    id_factura BIGINT NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    cantidad NUMERIC(12, 2) NOT NULL DEFAULT 1,
    precio_unitario NUMERIC(14, 2) NOT NULL,
    total_linea NUMERIC(14, 2) NOT NULL,

    CONSTRAINT pk_facturas_internas_detalles
        PRIMARY KEY (id_detalle),

    CONSTRAINT fk_facturas_internas_detalles_factura
        FOREIGN KEY (id_factura)
            REFERENCES soracare.facturas_internas (id_factura)
            ON DELETE CASCADE,

    CONSTRAINT chk_facturas_internas_detalle_descripcion
        CHECK (BTRIM(descripcion) <> ''),

    CONSTRAINT chk_facturas_internas_detalle_importes
        CHECK (
            cantidad > 0
            AND precio_unitario > 0
            AND total_linea = ROUND(cantidad * precio_unitario, 2)
        )
);

CREATE INDEX idx_facturas_internas_fecha
    ON soracare.facturas_internas (fecha_emision DESC);

CREATE INDEX idx_facturas_internas_detalles_factura
    ON soracare.facturas_internas_detalles (id_factura);

COMMENT ON TABLE soracare.facturas_internas IS
    'Documento comercial interno de SoraCare, sin validez tributaria y separado de e-Kuatia/SIFEN';

COMMENT ON COLUMN soracare.facturas_internas.validez_tributaria IS
    'Debe permanecer en FALSE hasta implementar una integracion fiscal homologada';

COMMENT ON COLUMN soracare.facturas_internas.numero_factura IS
    'Numeracion operativa interna; no corresponde a CDC, timbrado ni numero fiscal';

ALTER SEQUENCE soracare.facturas_internas_numero_seq OWNER TO soracare_app;
ALTER TABLE soracare.facturas_internas OWNER TO soracare_app;
ALTER TABLE soracare.facturas_internas_detalles OWNER TO soracare_app;

COMMIT;
