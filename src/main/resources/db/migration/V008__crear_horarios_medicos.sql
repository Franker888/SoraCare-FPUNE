BEGIN;

-- =========================================================
-- HORARIOS SEMANALES DE LOS MÉDICOS
-- =========================================================

CREATE TABLE soracare.horarios_medicos (
                                           id_horario BIGINT GENERATED ALWAYS AS IDENTITY,
                                           id_medico BIGINT NOT NULL,
                                           id_especialidad SMALLINT NOT NULL,
                                           id_consultorio BIGINT NOT NULL,
                                           dia_semana SMALLINT NOT NULL,
                                           hora_inicio TIME WITHOUT TIME ZONE NOT NULL,
                                           hora_fin TIME WITHOUT TIME ZONE NOT NULL,
                                           duracion_turno_minutos SMALLINT NOT NULL DEFAULT 30,
                                           fecha_desde DATE NOT NULL DEFAULT CURRENT_DATE,
                                           fecha_hasta DATE,
                                           activo BOOLEAN NOT NULL DEFAULT TRUE,
                                           fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL
                                                                                    DEFAULT CURRENT_TIMESTAMP,
                                           fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL
                                                                                    DEFAULT CURRENT_TIMESTAMP,

                                           CONSTRAINT pk_horarios_medicos
                                               PRIMARY KEY (id_horario),

                                           CONSTRAINT fk_horarios_medicos_especialidad
                                               FOREIGN KEY (id_medico, id_especialidad)
                                                   REFERENCES soracare.medicos_especialidades (
                                                                                               id_medico,
                                                                                               id_especialidad
                                                       )
                                                   ON UPDATE RESTRICT
                                                   ON DELETE RESTRICT,

                                           CONSTRAINT fk_horarios_medicos_consultorio
                                               FOREIGN KEY (id_consultorio)
                                                   REFERENCES soracare.consultorios (id_consultorio)
                                                   ON UPDATE RESTRICT
                                                   ON DELETE RESTRICT,

                                           CONSTRAINT chk_horarios_dia_semana
                                               CHECK (dia_semana BETWEEN 1 AND 7),

                                           CONSTRAINT chk_horarios_horas
                                               CHECK (hora_inicio < hora_fin),

                                           CONSTRAINT chk_horarios_duracion_turno
                                               CHECK (duracion_turno_minutos BETWEEN 10 AND 240),

                                           CONSTRAINT chk_horarios_duracion_disponible
                                               CHECK (
                                                   EXTRACT(
                                                           EPOCH FROM (hora_fin - hora_inicio)
                                                   ) / 60 >= duracion_turno_minutos
                                                   ),

                                           CONSTRAINT chk_horarios_fechas
                                               CHECK (
                                                   fecha_hasta IS NULL
                                                       OR fecha_hasta >= fecha_desde
                                                   )
);

CREATE UNIQUE INDEX uq_horarios_medicos_inicio
    ON soracare.horarios_medicos (
                                  id_medico,
                                  id_especialidad,
                                  id_consultorio,
                                  dia_semana,
                                  hora_inicio,
                                  fecha_desde
        );

CREATE INDEX idx_horarios_medico_dia
    ON soracare.horarios_medicos (
                                  id_medico,
                                  dia_semana,
                                  activo
        );

CREATE INDEX idx_horarios_consultorio_dia
    ON soracare.horarios_medicos (
                                  id_consultorio,
                                  dia_semana,
                                  activo
        );

COMMENT ON TABLE soracare.horarios_medicos IS
    'Horarios semanales recurrentes de atención de los médicos';

COMMENT ON COLUMN soracare.horarios_medicos.dia_semana IS
    'Día de la semana según ISO: 1 lunes, 2 martes, 3 miércoles, 4 jueves, 5 viernes, 6 sábado y 7 domingo';

COMMENT ON COLUMN soracare.horarios_medicos.duracion_turno_minutos IS
    'Duración en minutos de cada turno generado dentro del horario';

COMMENT ON COLUMN soracare.horarios_medicos.fecha_hasta IS
    'Fecha final opcional de vigencia del horario';


-- =========================================================
-- EXCEPCIONES DE LOS HORARIOS
-- =========================================================

CREATE TABLE soracare.excepciones_horarios_medicos (
                                                       id_excepcion BIGINT GENERATED ALWAYS AS IDENTITY,
                                                       id_medico BIGINT NOT NULL,
                                                       id_especialidad SMALLINT,
                                                       id_consultorio BIGINT,
                                                       fecha DATE NOT NULL,
                                                       hora_inicio TIME WITHOUT TIME ZONE,
                                                       hora_fin TIME WITHOUT TIME ZONE,
                                                       tipo VARCHAR(30) NOT NULL,
                                                       motivo VARCHAR(255) NOT NULL,
                                                       activo BOOLEAN NOT NULL DEFAULT TRUE,
                                                       fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL
                                                                               DEFAULT CURRENT_TIMESTAMP,
                                                       fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL
                                                                               DEFAULT CURRENT_TIMESTAMP,

                                                       CONSTRAINT pk_excepciones_horarios_medicos
                                                           PRIMARY KEY (id_excepcion),

                                                       CONSTRAINT fk_excepciones_medico
                                                           FOREIGN KEY (id_medico)
                                                               REFERENCES soracare.medicos (id_medico)
                                                               ON UPDATE RESTRICT
                                                               ON DELETE RESTRICT,

                                                       CONSTRAINT fk_excepciones_medico_especialidad
                                                           FOREIGN KEY (id_medico, id_especialidad)
                                                               REFERENCES soracare.medicos_especialidades (
                                                                                                           id_medico,
                                                                                                           id_especialidad
                                                                   )
                                                               ON UPDATE RESTRICT
                                                               ON DELETE RESTRICT,

                                                       CONSTRAINT fk_excepciones_consultorio
                                                           FOREIGN KEY (id_consultorio)
                                                               REFERENCES soracare.consultorios (id_consultorio)
                                                               ON UPDATE RESTRICT
                                                               ON DELETE RESTRICT,

                                                       CONSTRAINT chk_excepciones_tipo
                                                           CHECK (
                                                               tipo IN (
                                                                        'BLOQUEO_TOTAL',
                                                                        'BLOQUEO_PARCIAL',
                                                                        'DISPONIBILIDAD_EXTRA'
                                                                   )
                                                               ),

                                                       CONSTRAINT chk_excepciones_motivo
                                                           CHECK (BTRIM(motivo) <> ''),

                                                       CONSTRAINT chk_excepciones_datos_tipo
                                                           CHECK (
                                                               (
                                                                   tipo = 'BLOQUEO_TOTAL'
                                                                       AND hora_inicio IS NULL
                                                                       AND hora_fin IS NULL
                                                                   )
                                                                   OR
                                                               (
                                                                   tipo = 'BLOQUEO_PARCIAL'
                                                                       AND hora_inicio IS NOT NULL
                                                                       AND hora_fin IS NOT NULL
                                                                       AND hora_inicio < hora_fin
                                                                   )
                                                                   OR
                                                               (
                                                                   tipo = 'DISPONIBILIDAD_EXTRA'
                                                                       AND id_especialidad IS NOT NULL
                                                                       AND id_consultorio IS NOT NULL
                                                                       AND hora_inicio IS NOT NULL
                                                                       AND hora_fin IS NOT NULL
                                                                       AND hora_inicio < hora_fin
                                                                   )
                                                               )
);

CREATE INDEX idx_excepciones_medico_fecha
    ON soracare.excepciones_horarios_medicos (
                                              id_medico,
                                              fecha,
                                              activo
        );

CREATE INDEX idx_excepciones_consultorio_fecha
    ON soracare.excepciones_horarios_medicos (
                                              id_consultorio,
                                              fecha
        )
    WHERE id_consultorio IS NOT NULL;

COMMENT ON TABLE soracare.excepciones_horarios_medicos IS
    'Bloqueos y disponibilidades extraordinarias aplicadas a la agenda médica';

COMMENT ON COLUMN soracare.excepciones_horarios_medicos.tipo IS
    'BLOQUEO_TOTAL, BLOQUEO_PARCIAL o DISPONIBILIDAD_EXTRA';

COMMENT ON COLUMN soracare.excepciones_horarios_medicos.motivo IS
    'Motivo de la excepción, como vacaciones, feriado, licencia o reemplazo';


-- =========================================================
-- PROPIETARIO DE LAS TABLAS
-- =========================================================

ALTER TABLE soracare.horarios_medicos
    OWNER TO soracare_app;

ALTER TABLE soracare.excepciones_horarios_medicos
    OWNER TO soracare_app;

COMMIT;