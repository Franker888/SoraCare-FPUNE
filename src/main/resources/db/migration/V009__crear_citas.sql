BEGIN;

-- =========================================================
-- ESTADOS POSIBLES DE UNA CITA
-- =========================================================

CREATE TABLE soracare.estados_citas (
                                        id_estado_cita SMALLINT GENERATED ALWAYS AS IDENTITY,
                                        codigo VARCHAR(30) NOT NULL,
                                        nombre VARCHAR(60) NOT NULL,
                                        descripcion VARCHAR(255),
                                        es_final BOOLEAN NOT NULL DEFAULT FALSE,
                                        activo BOOLEAN NOT NULL DEFAULT TRUE,

                                        CONSTRAINT pk_estados_citas
                                            PRIMARY KEY (id_estado_cita),

                                        CONSTRAINT uq_estados_citas_codigo
                                            UNIQUE (codigo),

                                        CONSTRAINT chk_estados_citas_codigo
                                            CHECK (BTRIM(codigo) <> ''),

                                        CONSTRAINT chk_estados_citas_nombre
                                            CHECK (BTRIM(nombre) <> '')
);

COMMENT ON TABLE soracare.estados_citas IS
    'Catálogo de estados disponibles para las citas médicas';

COMMENT ON COLUMN soracare.estados_citas.es_final IS
    'Indica si el estado concluye el ciclo de vida de la cita';


-- =========================================================
-- ESTADOS INICIALES
-- =========================================================

INSERT INTO soracare.estados_citas
(codigo, nombre, descripcion, es_final)
VALUES
    (
        'SOLICITADA',
        'Solicitada',
        'La cita fue solicitada y todavía debe procesarse',
        FALSE
    ),
    (
        'PENDIENTE_PAGO',
        'Pendiente de pago',
        'La reserva espera la confirmación del pago',
        FALSE
    ),
    (
        'CONFIRMADA',
        'Confirmada',
        'La cita y su horario fueron confirmados',
        FALSE
    ),
    (
        'EN_ATENCION',
        'En atención',
        'El médico se encuentra atendiendo al paciente',
        FALSE
    ),
    (
        'ATENDIDA',
        'Atendida',
        'La consulta médica fue realizada correctamente',
        TRUE
    ),
    (
        'CANCELADA',
        'Cancelada',
        'La cita fue cancelada',
        TRUE
    ),
    (
        'AUSENTE',
        'Paciente ausente',
        'El paciente no se presentó a la consulta',
        TRUE
    );


-- =========================================================
-- TABLA PRINCIPAL DE CITAS
-- =========================================================

CREATE TABLE soracare.citas (
                                id_cita BIGINT GENERATED ALWAYS AS IDENTITY,
                                id_paciente BIGINT NOT NULL,
                                id_medico BIGINT NOT NULL,
                                id_especialidad SMALLINT NOT NULL,
                                id_consultorio BIGINT NOT NULL,
                                id_horario BIGINT,
                                id_estado_cita SMALLINT NOT NULL,
                                fecha_hora_inicio TIMESTAMP WITH TIME ZONE NOT NULL,
                                fecha_hora_fin TIMESTAMP WITH TIME ZONE NOT NULL,
                                motivo_consulta VARCHAR(500) NOT NULL,
                                observaciones VARCHAR(1000),
                                canal_reserva VARCHAR(20) NOT NULL DEFAULT 'WEB',
                                motivo_cancelacion VARCHAR(500),
                                id_usuario_creador BIGINT,
                                id_usuario_modificacion BIGINT,
                                fecha_creacion TIMESTAMP WITH TIME ZONE
                                    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                fecha_actualizacion TIMESTAMP WITH TIME ZONE
                                    NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT pk_citas
                                    PRIMARY KEY (id_cita),

                                CONSTRAINT fk_citas_paciente
                                    FOREIGN KEY (id_paciente)
                                        REFERENCES soracare.pacientes (id_paciente)
                                        ON UPDATE RESTRICT
                                        ON DELETE RESTRICT,

                                CONSTRAINT fk_citas_medico_especialidad
                                    FOREIGN KEY (id_medico, id_especialidad)
                                        REFERENCES soracare.medicos_especialidades
                                            (id_medico, id_especialidad)
                                        ON UPDATE RESTRICT
                                        ON DELETE RESTRICT,

                                CONSTRAINT fk_citas_consultorio
                                    FOREIGN KEY (id_consultorio)
                                        REFERENCES soracare.consultorios (id_consultorio)
                                        ON UPDATE RESTRICT
                                        ON DELETE RESTRICT,

                                CONSTRAINT fk_citas_horario
                                    FOREIGN KEY (id_horario)
                                        REFERENCES soracare.horarios_medicos (id_horario)
                                        ON UPDATE RESTRICT
                                        ON DELETE SET NULL,

                                CONSTRAINT fk_citas_estado
                                    FOREIGN KEY (id_estado_cita)
                                        REFERENCES soracare.estados_citas (id_estado_cita)
                                        ON UPDATE RESTRICT
                                        ON DELETE RESTRICT,

                                CONSTRAINT fk_citas_usuario_creador
                                    FOREIGN KEY (id_usuario_creador)
                                        REFERENCES soracare.usuarios (id_usuario)
                                        ON UPDATE RESTRICT
                                        ON DELETE SET NULL,

                                CONSTRAINT fk_citas_usuario_modificacion
                                    FOREIGN KEY (id_usuario_modificacion)
                                        REFERENCES soracare.usuarios (id_usuario)
                                        ON UPDATE RESTRICT
                                        ON DELETE SET NULL,

                                CONSTRAINT chk_citas_horas
                                    CHECK (fecha_hora_inicio < fecha_hora_fin),

                                CONSTRAINT chk_citas_duracion
                                    CHECK (
                                        EXTRACT(
                                                EPOCH FROM (fecha_hora_fin - fecha_hora_inicio)
                                        ) / 60 BETWEEN 10 AND 240
                                        ),

                                CONSTRAINT chk_citas_motivo
                                    CHECK (BTRIM(motivo_consulta) <> ''),

                                CONSTRAINT chk_citas_canal
                                    CHECK (
                                        canal_reserva IN (
                                                          'WEB',
                                                          'RECEPCION',
                                                          'TELEFONO',
                                                          'ADMINISTRACION'
                                            )
                                        ),

                                CONSTRAINT chk_citas_motivo_cancelacion
                                    CHECK (
                                        motivo_cancelacion IS NULL
                                            OR BTRIM(motivo_cancelacion) <> ''
                                        )
);


-- =========================================================
-- HISTORIAL DE ESTADOS DE LAS CITAS
-- =========================================================

CREATE TABLE soracare.historial_estados_citas (
                                                  id_historial BIGINT GENERATED ALWAYS AS IDENTITY,
                                                  id_cita BIGINT NOT NULL,
                                                  id_estado_anterior SMALLINT,
                                                  id_estado_nuevo SMALLINT NOT NULL,
                                                  id_usuario BIGINT,
                                                  motivo VARCHAR(500),
                                                  fecha_cambio TIMESTAMP WITH TIME ZONE
                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                                  CONSTRAINT pk_historial_estados_citas
                                                      PRIMARY KEY (id_historial),

                                                  CONSTRAINT fk_historial_cita
                                                      FOREIGN KEY (id_cita)
                                                          REFERENCES soracare.citas (id_cita)
                                                          ON UPDATE RESTRICT
                                                          ON DELETE RESTRICT,

                                                  CONSTRAINT fk_historial_estado_anterior
                                                      FOREIGN KEY (id_estado_anterior)
                                                          REFERENCES soracare.estados_citas (id_estado_cita)
                                                          ON UPDATE RESTRICT
                                                          ON DELETE RESTRICT,

                                                  CONSTRAINT fk_historial_estado_nuevo
                                                      FOREIGN KEY (id_estado_nuevo)
                                                          REFERENCES soracare.estados_citas (id_estado_cita)
                                                          ON UPDATE RESTRICT
                                                          ON DELETE RESTRICT,

                                                  CONSTRAINT fk_historial_usuario
                                                      FOREIGN KEY (id_usuario)
                                                          REFERENCES soracare.usuarios (id_usuario)
                                                          ON UPDATE RESTRICT
                                                          ON DELETE SET NULL,

                                                  CONSTRAINT chk_historial_motivo
                                                      CHECK (
                                                          motivo IS NULL
                                                              OR BTRIM(motivo) <> ''
                                                          )
);


-- =========================================================
-- VALIDACIÓN CONTRA CITAS SUPERPUESTAS
-- =========================================================

CREATE OR REPLACE FUNCTION soracare.validar_superposicion_cita()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
codigo_estado_nuevo VARCHAR(30);
BEGIN
SELECT estado.codigo
INTO codigo_estado_nuevo
FROM soracare.estados_citas estado
WHERE estado.id_estado_cita = NEW.id_estado_cita;

-- Una cita cancelada deja libre el horario.
IF codigo_estado_nuevo = 'CANCELADA' THEN
        RETURN NEW;
END IF;

    IF EXISTS (
        SELECT 1
          FROM soracare.citas cita
          JOIN soracare.estados_citas estado
            ON estado.id_estado_cita = cita.id_estado_cita
         WHERE cita.id_cita <> COALESCE(NEW.id_cita, 0)
           AND estado.codigo <> 'CANCELADA'
           AND NEW.fecha_hora_inicio < cita.fecha_hora_fin
           AND NEW.fecha_hora_fin > cita.fecha_hora_inicio
           AND (
                cita.id_medico = NEW.id_medico
                OR cita.id_consultorio = NEW.id_consultorio
                OR cita.id_paciente = NEW.id_paciente
           )
    ) THEN
        RAISE EXCEPTION
            'El médico, consultorio o paciente ya posee una cita en ese horario';
END IF;

RETURN NEW;
END;
$$;


CREATE TRIGGER trg_validar_superposicion_cita
    BEFORE INSERT OR UPDATE OF
    id_paciente,
                         id_medico,
                         id_consultorio,
                         id_estado_cita,
                         fecha_hora_inicio,
                         fecha_hora_fin
                     ON soracare.citas
                         FOR EACH ROW
                         EXECUTE FUNCTION soracare.validar_superposicion_cita();


-- =========================================================
-- ACTUALIZACIÓN AUTOMÁTICA DE LA FECHA
-- =========================================================

CREATE OR REPLACE FUNCTION soracare.actualizar_fecha_cita()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.fecha_actualizacion = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$;


CREATE TRIGGER trg_actualizar_fecha_cita
    BEFORE UPDATE
    ON soracare.citas
    FOR EACH ROW
    EXECUTE FUNCTION soracare.actualizar_fecha_cita();


-- =========================================================
-- REGISTRO AUTOMÁTICO DEL HISTORIAL DE ESTADOS
-- =========================================================

CREATE OR REPLACE FUNCTION soracare.registrar_historial_estado_cita()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO soracare.historial_estados_citas (
            id_cita,
            id_estado_anterior,
            id_estado_nuevo,
            id_usuario,
            motivo
        )
        VALUES (
            NEW.id_cita,
            NULL,
            NEW.id_estado_cita,
            NEW.id_usuario_creador,
            'Creación de la cita'
        );

    ELSIF TG_OP = 'UPDATE'
       AND OLD.id_estado_cita IS DISTINCT FROM NEW.id_estado_cita THEN

        INSERT INTO soracare.historial_estados_citas (
            id_cita,
            id_estado_anterior,
            id_estado_nuevo,
            id_usuario,
            motivo
        )
        VALUES (
            NEW.id_cita,
            OLD.id_estado_cita,
            NEW.id_estado_cita,
            NEW.id_usuario_modificacion,
            COALESCE(
                NEW.motivo_cancelacion,
                'Cambio de estado de la cita'
            )
        );
END IF;

RETURN NEW;
END;
$$;


CREATE TRIGGER trg_registrar_historial_estado_cita
    AFTER INSERT OR UPDATE OF id_estado_cita
                    ON soracare.citas
                        FOR EACH ROW
                        EXECUTE FUNCTION soracare.registrar_historial_estado_cita();


-- =========================================================
-- ÍNDICES
-- =========================================================

CREATE INDEX idx_citas_paciente_fecha
    ON soracare.citas (
                       id_paciente,
                       fecha_hora_inicio
        );

CREATE INDEX idx_citas_medico_fecha
    ON soracare.citas (
                       id_medico,
                       fecha_hora_inicio
        );

CREATE INDEX idx_citas_consultorio_fecha
    ON soracare.citas (
                       id_consultorio,
                       fecha_hora_inicio
        );

CREATE INDEX idx_citas_estado_fecha
    ON soracare.citas (
                       id_estado_cita,
                       fecha_hora_inicio
        );

CREATE INDEX idx_historial_cita_fecha
    ON soracare.historial_estados_citas (
                                         id_cita,
                                         fecha_cambio
        );


-- =========================================================
-- COMENTARIOS
-- =========================================================

COMMENT ON TABLE soracare.citas IS
    'Reservas de consultas médicas del sistema SoraCare-FPUNE';

COMMENT ON COLUMN soracare.citas.id_horario IS
    'Horario recurrente desde el cual se originó la cita, cuando corresponda';

COMMENT ON COLUMN soracare.citas.canal_reserva IS
    'Canal utilizado para crear la cita';

COMMENT ON COLUMN soracare.citas.motivo_consulta IS
    'Motivo inicial informado al solicitar la consulta';

COMMENT ON TABLE soracare.historial_estados_citas IS
    'Historial cronológico de cambios de estado de cada cita';


-- =========================================================
-- PROPIETARIOS
-- =========================================================

ALTER TABLE soracare.estados_citas
    OWNER TO soracare_app;

ALTER TABLE soracare.citas
    OWNER TO soracare_app;

ALTER TABLE soracare.historial_estados_citas
    OWNER TO soracare_app;

ALTER FUNCTION soracare.validar_superposicion_cita()
    OWNER TO soracare_app;

ALTER FUNCTION soracare.actualizar_fecha_cita()
    OWNER TO soracare_app;

ALTER FUNCTION soracare.registrar_historial_estado_cita()
    OWNER TO soracare_app;

COMMIT;