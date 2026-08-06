BEGIN;

-- =========================================================
-- 1. ATENCIONES MÉDICAS
-- =========================================================

CREATE TABLE IF NOT EXISTS soracare.atenciones_medicas (
                                                           id_atencion BIGINT GENERATED ALWAYS AS IDENTITY,

                                                           id_cita BIGINT NOT NULL,
                                                           id_paciente BIGINT NOT NULL,
                                                           id_medico BIGINT NOT NULL,

                                                           fecha_hora_inicio TIMESTAMP WITH TIME ZONE
                                                           NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                                           fecha_hora_fin TIMESTAMP WITH TIME ZONE,

                                                           motivo_consulta VARCHAR(500) NOT NULL,
    sintomas TEXT,
    antecedentes_relevantes TEXT,
    examen_fisico TEXT,
    evaluacion_clinica TEXT,
    plan_tratamiento TEXT,
    indicaciones TEXT,
    observaciones TEXT,

    requiere_seguimiento BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_proximo_control DATE,

    estado VARCHAR(20) NOT NULL DEFAULT 'EN_CURSO',

    fecha_creacion TIMESTAMP WITH TIME ZONE
    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    fecha_actualizacion TIMESTAMP WITH TIME ZONE
                                      NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      CONSTRAINT pk_atenciones_medicas
                                      PRIMARY KEY (id_atencion),

    CONSTRAINT ux_atenciones_medicas_cita
    UNIQUE (id_cita),

    CONSTRAINT fk_atenciones_medicas_cita
    FOREIGN KEY (id_cita)
    REFERENCES soracare.citas (id_cita)
                                  ON UPDATE RESTRICT
                                  ON DELETE RESTRICT,

    CONSTRAINT fk_atenciones_medicas_paciente
    FOREIGN KEY (id_paciente)
    REFERENCES soracare.pacientes (id_paciente)
                                  ON UPDATE RESTRICT
                                  ON DELETE RESTRICT,

    CONSTRAINT fk_atenciones_medicas_medico
    FOREIGN KEY (id_medico)
    REFERENCES soracare.medicos (id_medico)
                                  ON UPDATE RESTRICT
                                  ON DELETE RESTRICT,

    CONSTRAINT ck_atenciones_medicas_motivo
    CHECK (BTRIM(motivo_consulta) <> ''),

    CONSTRAINT ck_atenciones_medicas_estado
    CHECK (
              estado IN (
              'EN_CURSO',
              'FINALIZADA',
              'ANULADA'
                        )
    ),

    CONSTRAINT ck_atenciones_medicas_fechas
    CHECK (
              fecha_hora_fin IS NULL
              OR fecha_hora_fin >= fecha_hora_inicio
          ),

    CONSTRAINT ck_atenciones_medicas_estado_final
    CHECK (
              estado = 'EN_CURSO'
              OR fecha_hora_fin IS NOT NULL
          )
    );


-- =========================================================
-- 2. CATÁLOGO DE DIAGNÓSTICOS
-- =========================================================

CREATE TABLE IF NOT EXISTS soracare.diagnosticos (
                                                     id_diagnostico BIGINT GENERATED ALWAYS AS IDENTITY,

                                                     codigo_cie10 VARCHAR(15),
    nombre VARCHAR(200) NOT NULL,
    descripcion TEXT,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    fecha_creacion TIMESTAMP WITH TIME ZONE
                                 NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT pk_diagnosticos
                                 PRIMARY KEY (id_diagnostico),

    CONSTRAINT ck_diagnosticos_nombre
    CHECK (BTRIM(nombre) <> ''),

    CONSTRAINT ck_diagnosticos_codigo
    CHECK (
              codigo_cie10 IS NULL
              OR BTRIM(codigo_cie10) <> ''
    )
    );


-- =========================================================
-- 3. DIAGNÓSTICOS ASIGNADOS A CADA ATENCIÓN
-- =========================================================

CREATE TABLE IF NOT EXISTS soracare.atenciones_diagnosticos (
                                                                id_atencion BIGINT NOT NULL,
                                                                id_diagnostico BIGINT NOT NULL,

                                                                tipo VARCHAR(20) NOT NULL DEFAULT 'PRESUNTIVO',
    principal BOOLEAN NOT NULL DEFAULT FALSE,
    observaciones TEXT,

    fecha_registro TIMESTAMP WITH TIME ZONE
                                 NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT pk_atenciones_diagnosticos
                                 PRIMARY KEY (id_atencion, id_diagnostico),

    CONSTRAINT fk_atenciones_diagnosticos_atencion
    FOREIGN KEY (id_atencion)
    REFERENCES soracare.atenciones_medicas (id_atencion)
                             ON UPDATE RESTRICT
                             ON DELETE RESTRICT,

    CONSTRAINT fk_atenciones_diagnosticos_diagnostico
    FOREIGN KEY (id_diagnostico)
    REFERENCES soracare.diagnosticos (id_diagnostico)
                             ON UPDATE RESTRICT
                             ON DELETE RESTRICT,

    CONSTRAINT ck_atenciones_diagnosticos_tipo
    CHECK (
              tipo IN (
              'PRESUNTIVO',
              'CONFIRMADO',
              'DESCARTADO'
                      )
    )
    );


-- =========================================================
-- 4. ÍNDICES
-- =========================================================

CREATE INDEX IF NOT EXISTS ix_atenciones_paciente_fecha
    ON soracare.atenciones_medicas (
    id_paciente,
    fecha_hora_inicio DESC
    );

CREATE INDEX IF NOT EXISTS ix_atenciones_medico_fecha
    ON soracare.atenciones_medicas (
    id_medico,
    fecha_hora_inicio DESC
    );

CREATE INDEX IF NOT EXISTS ix_atenciones_estado
    ON soracare.atenciones_medicas (estado);

CREATE INDEX IF NOT EXISTS ix_atenciones_fecha
    ON soracare.atenciones_medicas (fecha_hora_inicio DESC);

CREATE UNIQUE INDEX IF NOT EXISTS ux_diagnosticos_codigo_cie10
    ON soracare.diagnosticos (UPPER(codigo_cie10))
    WHERE codigo_cie10 IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_diagnosticos_nombre
    ON soracare.diagnosticos (LOWER(nombre));

CREATE INDEX IF NOT EXISTS ix_atenciones_diagnosticos_diagnostico
    ON soracare.atenciones_diagnosticos (id_diagnostico);

CREATE UNIQUE INDEX IF NOT EXISTS ux_atencion_diagnostico_principal
    ON soracare.atenciones_diagnosticos (id_atencion)
    WHERE principal = TRUE;


-- =========================================================
-- 5. VALIDAR QUE EL PACIENTE PERTENEZCA A LA CITA
-- =========================================================

CREATE OR REPLACE FUNCTION soracare.validar_paciente_atencion()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
paciente_cita BIGINT;
BEGIN
SELECT id_paciente
INTO paciente_cita
FROM soracare.citas
WHERE id_cita = NEW.id_cita;

IF paciente_cita IS NULL THEN
        RAISE EXCEPTION
            'La cita % no existe',
            NEW.id_cita;
END IF;

    IF NEW.id_paciente <> paciente_cita THEN
        RAISE EXCEPTION
            'El paciente % no corresponde a la cita %',
            NEW.id_paciente,
            NEW.id_cita;
END IF;

RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_validar_paciente_atencion
    ON soracare.atenciones_medicas;

CREATE TRIGGER trg_validar_paciente_atencion
    BEFORE INSERT OR UPDATE OF id_cita, id_paciente
                     ON soracare.atenciones_medicas
                         FOR EACH ROW
                         EXECUTE FUNCTION soracare.validar_paciente_atencion();


-- =========================================================
-- 6. ACTUALIZAR AUTOMÁTICAMENTE FECHA DE MODIFICACIÓN
-- =========================================================

CREATE OR REPLACE FUNCTION soracare.actualizar_fecha_atencion()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.fecha_actualizacion = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_actualizar_fecha_atencion
    ON soracare.atenciones_medicas;

CREATE TRIGGER trg_actualizar_fecha_atencion
    BEFORE UPDATE
    ON soracare.atenciones_medicas
    FOR EACH ROW
    EXECUTE FUNCTION soracare.actualizar_fecha_atencion();


-- =========================================================
-- 7. COMENTARIOS
-- =========================================================

COMMENT ON TABLE soracare.atenciones_medicas IS
    'Registro profesional de las atenciones médicas realizadas a los pacientes';

COMMENT ON COLUMN soracare.atenciones_medicas.id_medico IS
    'Médico que atendió realmente al paciente, aunque sea diferente del médico inicialmente programado';

COMMENT ON COLUMN soracare.atenciones_medicas.motivo_consulta IS
    'Razón principal informada para la atención médica';

COMMENT ON COLUMN soracare.atenciones_medicas.evaluacion_clinica IS
    'Valoración clínica realizada por el médico';

COMMENT ON COLUMN soracare.atenciones_medicas.plan_tratamiento IS
    'Plan terapéutico definido durante la atención';

COMMENT ON TABLE soracare.diagnosticos IS
    'Catálogo de diagnósticos médicos, compatible con códigos CIE-10';

COMMENT ON TABLE soracare.atenciones_diagnosticos IS
    'Relación entre las atenciones médicas y sus diagnósticos';


-- =========================================================
-- 8. PROPIETARIOS
-- =========================================================

ALTER TABLE soracare.atenciones_medicas
    OWNER TO soracare_app;

ALTER TABLE soracare.diagnosticos
    OWNER TO soracare_app;

ALTER TABLE soracare.atenciones_diagnosticos
    OWNER TO soracare_app;

ALTER FUNCTION soracare.validar_paciente_atencion()
    OWNER TO soracare_app;

ALTER FUNCTION soracare.actualizar_fecha_atencion()
    OWNER TO soracare_app;

COMMIT;