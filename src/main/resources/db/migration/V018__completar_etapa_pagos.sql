BEGIN;

-- =========================================================
-- ETAPA 5: TARIFAS, FACTURACION Y CONFIRMACION DE ASISTENCIA
-- =========================================================

ALTER TABLE soracare.medicos_especialidades
    ADD COLUMN tarifa_consulta NUMERIC(14, 2)
        NOT NULL DEFAULT 150000.00;

ALTER TABLE soracare.medicos_especialidades
    ADD CONSTRAINT chk_medicos_especialidades_tarifa
        CHECK (tarifa_consulta > 0);

COMMENT ON COLUMN soracare.medicos_especialidades.tarifa_consulta IS
    'Tarifa academica de la consulta en guaranies para este medico y especialidad';


CREATE TABLE soracare.datos_facturacion_pago (
    id_pago BIGINT NOT NULL,
    tipo_documento VARCHAR(15) NOT NULL,
    numero_documento VARCHAR(30) NOT NULL,
    nombre_razon_social VARCHAR(160) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    correo VARCHAR(255) NOT NULL,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_datos_facturacion_pago
        PRIMARY KEY (id_pago),

    CONSTRAINT fk_datos_facturacion_pago
        FOREIGN KEY (id_pago)
            REFERENCES soracare.pagos (id_pago)
            ON DELETE CASCADE,

    CONSTRAINT chk_datos_facturacion_tipo_documento
        CHECK (tipo_documento IN ('CI', 'RUC', 'PASAPORTE')),

    CONSTRAINT chk_datos_facturacion_numero
        CHECK (BTRIM(numero_documento) <> ''),

    CONSTRAINT chk_datos_facturacion_nombre
        CHECK (BTRIM(nombre_razon_social) <> ''),

    CONSTRAINT chk_datos_facturacion_direccion
        CHECK (BTRIM(direccion) <> ''),

    CONSTRAINT chk_datos_facturacion_correo
        CHECK (BTRIM(correo) <> '')
);

COMMENT ON TABLE soracare.datos_facturacion_pago IS
    'Copia inmutable de los datos declarados por el paciente para el comprobante academico';


ALTER TABLE soracare.comprobantes_pago
    ADD COLUMN contenido_pdf BYTEA,
    ADD COLUMN email_destino VARCHAR(255),
    ADD COLUMN estado_envio_email VARCHAR(20) NOT NULL DEFAULT 'NO_SOLICITADO';

ALTER TABLE soracare.comprobantes_pago
    ADD CONSTRAINT chk_comprobantes_estado_email
        CHECK (estado_envio_email IN ('NO_SOLICITADO', 'SIMULADO_ENVIADO', 'FALLIDO'));

CREATE UNIQUE INDEX ux_comprobantes_pago_unico
    ON soracare.comprobantes_pago (id_pago);

COMMENT ON COLUMN soracare.comprobantes_pago.contenido_pdf IS
    'Contenido PDF del comprobante academico, almacenado para descarga segura';

COMMENT ON COLUMN soracare.comprobantes_pago.estado_envio_email IS
    'Estado del envio academico; no implica entrega por un proveedor real de correo';


CREATE TABLE soracare.confirmaciones_asistencia (
    id_confirmacion BIGINT GENERATED ALWAYS AS IDENTITY,
    id_cita BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    token_hash CHAR(64),
    fecha_programada_primer_aviso TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_programada_segundo_aviso TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_primer_aviso TIMESTAMP WITH TIME ZONE,
    fecha_segundo_aviso TIMESTAMP WITH TIME ZONE,
    fecha_respuesta TIMESTAMP WITH TIME ZONE,
    fecha_vencimiento TIMESTAMP WITH TIME ZONE NOT NULL,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_confirmaciones_asistencia
        PRIMARY KEY (id_confirmacion),

    CONSTRAINT uq_confirmaciones_asistencia_cita
        UNIQUE (id_cita),

    CONSTRAINT fk_confirmaciones_asistencia_cita
        FOREIGN KEY (id_cita)
            REFERENCES soracare.citas (id_cita)
            ON DELETE CASCADE,

    CONSTRAINT chk_confirmaciones_asistencia_estado
        CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'RECHAZADA', 'VENCIDA')),

    CONSTRAINT chk_confirmaciones_asistencia_fechas
        CHECK (
            fecha_programada_primer_aviso <= fecha_programada_segundo_aviso
            AND fecha_programada_segundo_aviso <= fecha_vencimiento
        ),

    CONSTRAINT chk_confirmaciones_asistencia_token
        CHECK (token_hash IS NULL OR token_hash ~ '^[A-Fa-f0-9]{64}$')
);

CREATE INDEX idx_confirmaciones_asistencia_pendientes
    ON soracare.confirmaciones_asistencia (estado, fecha_programada_primer_aviso);

COMMENT ON TABLE soracare.confirmaciones_asistencia IS
    'Preparacion del flujo SMS: primer aviso a las 24 horas y vencimiento 12 horas antes de la cita';


ALTER TABLE soracare.datos_facturacion_pago OWNER TO soracare_app;
ALTER TABLE soracare.confirmaciones_asistencia OWNER TO soracare_app;

COMMIT;
