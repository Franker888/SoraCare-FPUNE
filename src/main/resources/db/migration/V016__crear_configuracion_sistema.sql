BEGIN;

-- =========================================================
-- TABLA PRINCIPAL DE CONFIGURACIONES
-- =========================================================

CREATE TABLE IF NOT EXISTS soracare.configuraciones_sistema (
                                                                id_configuracion BIGINT GENERATED ALWAYS AS IDENTITY,
                                                                clave VARCHAR(100) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    valor TEXT NOT NULL,
    tipo_dato VARCHAR(20) NOT NULL DEFAULT 'TEXTO',
    categoria VARCHAR(50) NOT NULL,
    descripcion VARCHAR(500),
    es_publica BOOLEAN NOT NULL DEFAULT FALSE,
    es_editable BOOLEAN NOT NULL DEFAULT TRUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP WITH TIME ZONE
    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE
                                      NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      CONSTRAINT pk_configuraciones_sistema
                                      PRIMARY KEY (id_configuracion),

    CONSTRAINT uq_configuraciones_sistema_clave
    UNIQUE (clave),

    CONSTRAINT ck_configuraciones_tipo_dato
    CHECK (
              tipo_dato IN (
              'TEXTO',
              'ENTERO',
              'DECIMAL',
              'BOOLEANO',
              'FECHA',
              'HORA',
              'JSON'
                           )
    )
    );

-- =========================================================
-- HISTORIAL DE CAMBIOS DE CONFIGURACIONES
-- =========================================================

CREATE TABLE IF NOT EXISTS soracare.historial_configuraciones (
                                                                  id_historial BIGINT GENERATED ALWAYS AS IDENTITY,
                                                                  id_configuracion BIGINT NOT NULL,
                                                                  valor_anterior TEXT,
                                                                  valor_nuevo TEXT,
                                                                  id_usuario BIGINT,
                                                                  motivo VARCHAR(500),
    fecha_cambio TIMESTAMP WITH TIME ZONE
                               NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT pk_historial_configuraciones
                               PRIMARY KEY (id_historial),

    CONSTRAINT fk_historial_configuracion
    FOREIGN KEY (id_configuracion)
    REFERENCES soracare.configuraciones_sistema(id_configuracion)
                           ON UPDATE CASCADE
                           ON DELETE RESTRICT,

    CONSTRAINT fk_historial_configuracion_usuario
    FOREIGN KEY (id_usuario)
    REFERENCES soracare.usuarios(id_usuario)
                           ON UPDATE CASCADE
                           ON DELETE SET NULL
    );

-- =========================================================
-- ÍNDICES
-- =========================================================

CREATE INDEX IF NOT EXISTS ix_configuraciones_categoria
    ON soracare.configuraciones_sistema(categoria);

CREATE INDEX IF NOT EXISTS ix_configuraciones_activas
    ON soracare.configuraciones_sistema(activo);

CREATE INDEX IF NOT EXISTS ix_historial_configuracion
    ON soracare.historial_configuraciones(id_configuracion);

CREATE INDEX IF NOT EXISTS ix_historial_configuracion_fecha
    ON soracare.historial_configuraciones(fecha_cambio);

-- =========================================================
-- ACTUALIZAR AUTOMÁTICAMENTE LA FECHA
-- =========================================================

CREATE OR REPLACE FUNCTION soracare.actualizar_fecha_configuracion()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.fecha_actualizacion := CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_actualizar_fecha_configuracion
    ON soracare.configuraciones_sistema;

CREATE TRIGGER trg_actualizar_fecha_configuracion
    BEFORE UPDATE ON soracare.configuraciones_sistema
    FOR EACH ROW
    EXECUTE FUNCTION soracare.actualizar_fecha_configuracion();

-- =========================================================
-- REGISTRAR AUTOMÁTICAMENTE CAMBIOS DE VALOR
-- =========================================================

CREATE OR REPLACE FUNCTION soracare.registrar_historial_configuracion()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
usuario_actual BIGINT;
    motivo_actual TEXT;
BEGIN
BEGIN
        usuario_actual :=
            NULLIF(
                current_setting('soracare.id_usuario', TRUE),
                ''
            )::BIGINT;
EXCEPTION
        WHEN invalid_text_representation THEN
            usuario_actual := NULL;
END;

    motivo_actual :=
        NULLIF(
            current_setting(
                'soracare.motivo_configuracion',
                TRUE
            ),
            ''
        );

    IF OLD.valor IS DISTINCT FROM NEW.valor THEN
        INSERT INTO soracare.historial_configuraciones (
            id_configuracion,
            valor_anterior,
            valor_nuevo,
            id_usuario,
            motivo
        )
        VALUES (
            NEW.id_configuracion,
            OLD.valor,
            NEW.valor,
            usuario_actual,
            motivo_actual
        );
END IF;

RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_registrar_historial_configuracion
    ON soracare.configuraciones_sistema;

CREATE TRIGGER trg_registrar_historial_configuracion
    AFTER UPDATE OF valor ON soracare.configuraciones_sistema
    FOR EACH ROW
    EXECUTE FUNCTION soracare.registrar_historial_configuracion();

-- =========================================================
-- CONFIGURACIONES INICIALES
-- =========================================================

INSERT INTO soracare.configuraciones_sistema (
    clave,
    nombre,
    valor,
    tipo_dato,
    categoria,
    descripcion,
    es_publica
)
VALUES
    (
        'SISTEMA_NOMBRE',
        'Nombre del sistema',
        'SoraCare-FPUNE',
        'TEXTO',
        'GENERAL',
        'Nombre utilizado en la interfaz y documentos',
        TRUE
    ),
    (
        'INSTITUCION_NOMBRE',
        'Nombre de la institución',
        'SoraCare-FPUNE',
        'TEXTO',
        'INSTITUCION',
        'Nombre oficial de la institución hospitalaria',
        TRUE
    ),
    (
        'ZONA_HORARIA',
        'Zona horaria',
        'America/Asuncion',
        'TEXTO',
        'REGIONAL',
        'Zona horaria utilizada para citas y operaciones',
        FALSE
    ),
    (
        'MONEDA',
        'Moneda del sistema',
        'PYG',
        'TEXTO',
        'REGIONAL',
        'Código de la moneda utilizada para los pagos',
        TRUE
    ),
    (
        'DURACION_CITA_MINUTOS',
        'Duración predeterminada de una cita',
        '30',
        'ENTERO',
        'CITAS',
        'Duración predeterminada de las citas médicas',
        FALSE
    ),
    (
        'ANTICIPACION_CANCELACION_HORAS',
        'Anticipación para cancelar una cita',
        '24',
        'ENTERO',
        'CITAS',
        'Horas mínimas requeridas para cancelar una cita',
        TRUE
    ),
    (
        'VENCIMIENTO_PAGO_MINUTOS',
        'Vencimiento de pago',
        '30',
        'ENTERO',
        'PAGOS',
        'Minutos disponibles para completar un pago pendiente',
        FALSE
    ),
    (
        'RECORDATORIO_CITA_HORAS',
        'Anticipación del recordatorio',
        '24',
        'ENTERO',
        'NOTIFICACIONES',
        'Horas de anticipación para recordar una cita',
        FALSE
    ),
    (
        'NOTIFICACIONES_EMAIL_HABILITADAS',
        'Notificaciones por correo',
        'true',
        'BOOLEANO',
        'NOTIFICACIONES',
        'Indica si el envío de correos está habilitado',
        FALSE
    ),
    (
        'NOTIFICACIONES_SMS_HABILITADAS',
        'Notificaciones por SMS',
        'false',
        'BOOLEANO',
        'NOTIFICACIONES',
        'Se activará cuando configuremos el proveedor de SMS',
        FALSE
    ),
    (
        'AUDITORIA_HABILITADA',
        'Auditoría del sistema',
        'true',
        'BOOLEANO',
        'SEGURIDAD',
        'Indica si el registro de auditoría está habilitado',
        FALSE
    )
    ON CONFLICT (clave) DO NOTHING;

-- =========================================================
-- COMENTARIOS
-- =========================================================

COMMENT ON TABLE soracare.configuraciones_sistema IS
    'Parámetros configurables para el funcionamiento de SoraCare-FPUNE';

COMMENT ON TABLE soracare.historial_configuraciones IS
    'Historial de modificaciones realizadas sobre configuraciones';

COMMENT ON COLUMN soracare.configuraciones_sistema.es_publica IS
    'Indica si la configuración puede mostrarse sin autenticación';

COMMENT ON COLUMN soracare.configuraciones_sistema.es_editable IS
    'Indica si un administrador puede modificarla desde el sistema';

-- =========================================================
-- PROPIETARIOS
-- =========================================================

ALTER TABLE soracare.configuraciones_sistema
    OWNER TO soracare_app;

ALTER TABLE soracare.historial_configuraciones
    OWNER TO soracare_app;

ALTER FUNCTION soracare.actualizar_fecha_configuracion()
    OWNER TO soracare_app;

ALTER FUNCTION soracare.registrar_historial_configuracion()
    OWNER TO soracare_app;

COMMIT;