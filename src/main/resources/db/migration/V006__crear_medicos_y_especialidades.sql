BEGIN;

-- =========================================================
-- TABLA DE ESPECIALIDADES
-- =========================================================

CREATE TABLE soracare.especialidades (
                                         id_especialidad SMALLINT GENERATED ALWAYS AS IDENTITY,
                                         nombre VARCHAR(100) NOT NULL,
                                         descripcion VARCHAR(500),
                                         duracion_consulta_minutos SMALLINT NOT NULL DEFAULT 30,
                                         activo BOOLEAN NOT NULL DEFAULT TRUE,
                                         fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                         CONSTRAINT pk_especialidades
                                             PRIMARY KEY (id_especialidad),

                                         CONSTRAINT chk_especialidades_nombre
                                             CHECK (BTRIM(nombre) <> ''),

                                         CONSTRAINT chk_especialidades_duracion
                                             CHECK (duracion_consulta_minutos BETWEEN 10 AND 240)
);

CREATE UNIQUE INDEX uq_especialidades_nombre_normalizado
    ON soracare.especialidades (LOWER(BTRIM(nombre)));

COMMENT ON TABLE soracare.especialidades IS
    'Catálogo de especialidades médicas disponibles en SoraCare-FPUNE';

COMMENT ON COLUMN soracare.especialidades.duracion_consulta_minutos IS
    'Duración estimada de una consulta de esta especialidad';


-- =========================================================
-- TABLA DE MÉDICOS
-- =========================================================

CREATE TABLE soracare.medicos (
                                  id_medico BIGINT GENERATED ALWAYS AS IDENTITY,
                                  id_persona BIGINT NOT NULL,
                                  matricula_profesional VARCHAR(50) NOT NULL,
                                  fecha_ingreso DATE,
                                  estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
                                  biografia VARCHAR(1000),
                                  fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT pk_medicos
                                      PRIMARY KEY (id_medico),

                                  CONSTRAINT uq_medicos_persona
                                      UNIQUE (id_persona),

                                  CONSTRAINT fk_medicos_personas
                                      FOREIGN KEY (id_persona)
                                          REFERENCES soracare.personas (id_persona)
                                          ON UPDATE RESTRICT
                                          ON DELETE RESTRICT,

                                  CONSTRAINT chk_medicos_matricula
                                      CHECK (BTRIM(matricula_profesional) <> ''),

                                  CONSTRAINT chk_medicos_estado
                                      CHECK (estado IN ('ACTIVO', 'INACTIVO', 'LICENCIA'))
);

CREATE UNIQUE INDEX uq_medicos_matricula_normalizada
    ON soracare.medicos (LOWER(BTRIM(matricula_profesional)));

CREATE INDEX idx_medicos_estado
    ON soracare.medicos (estado);

COMMENT ON TABLE soracare.medicos IS
    'Información profesional de las personas que trabajan como médicos';

COMMENT ON COLUMN soracare.medicos.id_persona IS
    'Persona correspondiente al profesional médico';

COMMENT ON COLUMN soracare.medicos.matricula_profesional IS
    'Número único de matrícula profesional del médico';


-- =========================================================
-- RELACIÓN ENTRE MÉDICOS Y ESPECIALIDADES
-- =========================================================

CREATE TABLE soracare.medicos_especialidades (
                                                 id_medico BIGINT NOT NULL,
                                                 id_especialidad SMALLINT NOT NULL,
                                                 es_principal BOOLEAN NOT NULL DEFAULT FALSE,
                                                 fecha_desde DATE NOT NULL DEFAULT CURRENT_DATE,
                                                 fecha_hasta DATE,
                                                 activo BOOLEAN NOT NULL DEFAULT TRUE,
                                                 fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                                 CONSTRAINT pk_medicos_especialidades
                                                     PRIMARY KEY (id_medico, id_especialidad),

                                                 CONSTRAINT fk_medicos_especialidades_medico
                                                     FOREIGN KEY (id_medico)
                                                         REFERENCES soracare.medicos (id_medico)
                                                         ON UPDATE RESTRICT
                                                         ON DELETE RESTRICT,

                                                 CONSTRAINT fk_medicos_especialidades_especialidad
                                                     FOREIGN KEY (id_especialidad)
                                                         REFERENCES soracare.especialidades (id_especialidad)
                                                         ON UPDATE RESTRICT
                                                         ON DELETE RESTRICT,

                                                 CONSTRAINT chk_medicos_especialidades_fechas
                                                     CHECK (fecha_hasta IS NULL OR fecha_hasta >= fecha_desde)
);

CREATE INDEX idx_medicos_especialidades_especialidad
    ON soracare.medicos_especialidades (id_especialidad);

CREATE UNIQUE INDEX uq_medico_especialidad_principal
    ON soracare.medicos_especialidades (id_medico)
    WHERE es_principal = TRUE AND activo = TRUE;

COMMENT ON TABLE soracare.medicos_especialidades IS
    'Especialidades que posee cada médico';

COMMENT ON COLUMN soracare.medicos_especialidades.es_principal IS
    'Indica cuál es la especialidad principal del médico';


-- =========================================================
-- PROPIETARIO DE LAS TABLAS
-- =========================================================

ALTER TABLE soracare.especialidades
    OWNER TO soracare_app;

ALTER TABLE soracare.medicos
    OWNER TO soracare_app;

ALTER TABLE soracare.medicos_especialidades
    OWNER TO soracare_app;

COMMIT;