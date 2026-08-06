BEGIN;

-- =========================================================
-- TABLA: auditoria_eventos
-- Registra las acciones importantes realizadas en el sistema.
-- =========================================================

CREATE TABLE soracare.auditoria_eventos (
                                            id_auditoria BIGINT
                                                GENERATED ALWAYS AS IDENTITY,

    -- Puede ser nulo en accesos públicos o intentos fallidos.
                                            id_usuario BIGINT,

                                            tipo_evento VARCHAR(30) NOT NULL,

                                            modulo VARCHAR(50) NOT NULL,

                                            entidad VARCHAR(100) NOT NULL,

    -- Se utiliza VARCHAR porque diferentes tablas pueden
    -- utilizar identificadores de distintos tipos.
                                            id_registro VARCHAR(100),

                                            descripcion VARCHAR(500),

    -- Permiten conocer qué información cambió.
                                            datos_anteriores JSONB,
                                            datos_nuevos JSONB,

                                            direccion_ip INET,

                                            metodo_http VARCHAR(10),
                                            ruta_solicitud VARCHAR(300),
                                            agente_usuario VARCHAR(500),

                                            resultado VARCHAR(20)
                                                                    NOT NULL DEFAULT 'EXITOSO',

                                            mensaje_error TEXT,

    -- Usuario técnico de PostgreSQL que realizó la operación.
                                            usuario_base_datos VARCHAR(100)
                                                                    NOT NULL DEFAULT CURRENT_USER,

                                            id_transaccion BIGINT
                                                                    NOT NULL DEFAULT TXID_CURRENT(),

                                            fecha_evento TIMESTAMP WITH TIME ZONE
                                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                            CONSTRAINT pk_auditoria_eventos
                                                PRIMARY KEY (id_auditoria),

                                            CONSTRAINT fk_auditoria_usuario
                                                FOREIGN KEY (id_usuario)
                                                    REFERENCES soracare.usuarios (id_usuario)
                                                    ON UPDATE CASCADE
                                                    ON DELETE RESTRICT,

                                            CONSTRAINT ck_auditoria_tipo_evento
                                                CHECK (
                                                    tipo_evento IN (
                                                                    'CREACION',
                                                                    'MODIFICACION',
                                                                    'ELIMINACION',
                                                                    'CONSULTA',
                                                                    'INICIO_SESION',
                                                                    'CIERRE_SESION',
                                                                    'AUTENTICACION_FALLIDA',
                                                                    'ASIGNACION_ROL',
                                                                    'EXPORTACION',
                                                                    'ERROR'
                                                        )
                                                    ),

                                            CONSTRAINT ck_auditoria_modulo
                                                CHECK (BTRIM(modulo) <> ''),

                                            CONSTRAINT ck_auditoria_entidad
                                                CHECK (BTRIM(entidad) <> ''),

                                            CONSTRAINT ck_auditoria_resultado
                                                CHECK (
                                                    resultado IN (
                                                                  'EXITOSO',
                                                                  'FALLIDO'
                                                        )
                                                    ),

                                            CONSTRAINT ck_auditoria_metodo_http
                                                CHECK (
                                                    metodo_http IS NULL
                                                        OR metodo_http IN (
                                                                           'GET',
                                                                           'POST',
                                                                           'PUT',
                                                                           'PATCH',
                                                                           'DELETE',
                                                                           'HEAD',
                                                                           'OPTIONS'
                                                        )
                                                    )
);


-- =========================================================
-- ÍNDICES PARA FACILITAR LA BÚSQUEDA DE LA AUDITORÍA
-- =========================================================

CREATE INDEX idx_auditoria_usuario
    ON soracare.auditoria_eventos (id_usuario)
    WHERE id_usuario IS NOT NULL;

CREATE INDEX idx_auditoria_fecha
    ON soracare.auditoria_eventos (fecha_evento DESC);

CREATE INDEX idx_auditoria_tipo_evento
    ON soracare.auditoria_eventos (tipo_evento);

CREATE INDEX idx_auditoria_entidad_registro
    ON soracare.auditoria_eventos (
                                   entidad,
                                   id_registro
        );


-- =========================================================
-- PROTECCIÓN DE LA AUDITORÍA
-- Evita que los registros sean modificados o eliminados.
-- =========================================================

CREATE OR REPLACE FUNCTION soracare.impedir_modificacion_auditoria()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'Los registros de auditoría no pueden modificarse ni eliminarse';
END;
$$;

CREATE TRIGGER trg_auditoria_inmutable
    BEFORE UPDATE OR DELETE
ON soracare.auditoria_eventos
FOR EACH ROW
EXECUTE FUNCTION soracare.impedir_modificacion_auditoria();


-- =========================================================
-- COMENTARIOS
-- =========================================================

COMMENT ON TABLE soracare.auditoria_eventos IS
    'Bitácora inmutable de acciones realizadas en SoraCare-FPUNE';

COMMENT ON COLUMN soracare.auditoria_eventos.datos_anteriores IS
    'Datos del registro antes de una modificación o eliminación';

COMMENT ON COLUMN soracare.auditoria_eventos.datos_nuevos IS
    'Datos del registro después de una creación o modificación';

COMMENT ON COLUMN soracare.auditoria_eventos.tipo_evento IS
    'Acción ejecutada por el usuario dentro del sistema';

COMMENT ON FUNCTION soracare.impedir_modificacion_auditoria() IS
    'Impide actualizar o eliminar registros de auditoría';


-- =========================================================
-- PROPIETARIO
-- =========================================================

ALTER TABLE soracare.auditoria_eventos
    OWNER TO soracare_app;

ALTER FUNCTION soracare.impedir_modificacion_auditoria()
    OWNER TO soracare_app;

COMMIT;