BEGIN;

-- =========================================================
-- TABLA DE SEDES
-- =========================================================

CREATE TABLE soracare.sedes (
                                id_sede SMALLINT GENERATED ALWAYS AS IDENTITY,
                                codigo VARCHAR(20) NOT NULL,
                                nombre VARCHAR(120) NOT NULL,
                                direccion VARCHAR(255) NOT NULL,
                                ciudad VARCHAR(100) NOT NULL,
                                departamento VARCHAR(100),
                                telefono VARCHAR(30),
                                correo VARCHAR(150),
                                activo BOOLEAN NOT NULL DEFAULT TRUE,
                                fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL
                                                        DEFAULT CURRENT_TIMESTAMP,
                                fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL
                                                        DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT pk_sedes
                                    PRIMARY KEY (id_sede),

                                CONSTRAINT chk_sedes_codigo
                                    CHECK (BTRIM(codigo) <> ''),

                                CONSTRAINT chk_sedes_nombre
                                    CHECK (BTRIM(nombre) <> ''),

                                CONSTRAINT chk_sedes_direccion
                                    CHECK (BTRIM(direccion) <> ''),

                                CONSTRAINT chk_sedes_ciudad
                                    CHECK (BTRIM(ciudad) <> '')
);

CREATE UNIQUE INDEX uq_sedes_codigo_normalizado
    ON soracare.sedes (LOWER(BTRIM(codigo)));

CREATE UNIQUE INDEX uq_sedes_nombre_normalizado
    ON soracare.sedes (LOWER(BTRIM(nombre)));

COMMENT ON TABLE soracare.sedes IS
    'Ubicaciones físicas o sucursales pertenecientes al sistema hospitalario';

COMMENT ON COLUMN soracare.sedes.codigo IS
    'Código único utilizado para identificar la sede';

COMMENT ON COLUMN soracare.sedes.activo IS
    'Indica si la sede se encuentra disponible para operar';


-- =========================================================
-- TABLA DE CONSULTORIOS
-- =========================================================

CREATE TABLE soracare.consultorios (
                                       id_consultorio BIGINT GENERATED ALWAYS AS IDENTITY,
                                       id_sede SMALLINT NOT NULL,
                                       codigo VARCHAR(30) NOT NULL,
                                       nombre VARCHAR(100) NOT NULL,
                                       piso VARCHAR(20),
                                       ubicacion_referencia VARCHAR(255),
                                       tipo VARCHAR(30) NOT NULL DEFAULT 'CONSULTA',
                                       capacidad SMALLINT NOT NULL DEFAULT 1,
                                       accesible BOOLEAN NOT NULL DEFAULT FALSE,
                                       estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
                                       fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL
                                           DEFAULT CURRENT_TIMESTAMP,
                                       fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL
                                           DEFAULT CURRENT_TIMESTAMP,

                                       CONSTRAINT pk_consultorios
                                           PRIMARY KEY (id_consultorio),

                                       CONSTRAINT fk_consultorios_sedes
                                           FOREIGN KEY (id_sede)
                                               REFERENCES soracare.sedes (id_sede)
                                               ON UPDATE RESTRICT
                                               ON DELETE RESTRICT,

                                       CONSTRAINT chk_consultorios_codigo
                                           CHECK (BTRIM(codigo) <> ''),

                                       CONSTRAINT chk_consultorios_nombre
                                           CHECK (BTRIM(nombre) <> ''),

                                       CONSTRAINT chk_consultorios_tipo
                                           CHECK (
                                               tipo IN (
                                                        'CONSULTA',
                                                        'PROCEDIMIENTO',
                                                        'DIAGNOSTICO',
                                                        'EMERGENCIA'
                                                   )
                                               ),

                                       CONSTRAINT chk_consultorios_capacidad
                                           CHECK (capacidad BETWEEN 1 AND 50),

                                       CONSTRAINT chk_consultorios_estado
                                           CHECK (
                                               estado IN (
                                                          'DISPONIBLE',
                                                          'MANTENIMIENTO',
                                                          'INACTIVO'
                                                   )
                                               )
);

CREATE UNIQUE INDEX uq_consultorios_sede_codigo
    ON soracare.consultorios (
                              id_sede,
                              LOWER(BTRIM(codigo))
        );

CREATE INDEX idx_consultorios_sede
    ON soracare.consultorios (id_sede);

CREATE INDEX idx_consultorios_estado
    ON soracare.consultorios (estado);

COMMENT ON TABLE soracare.consultorios IS
    'Consultorios y salas físicas disponibles dentro de cada sede';

COMMENT ON COLUMN soracare.consultorios.tipo IS
    'Uso principal del consultorio o sala';

COMMENT ON COLUMN soracare.consultorios.accesible IS
    'Indica si el consultorio posee condiciones de accesibilidad';

COMMENT ON COLUMN soracare.consultorios.estado IS
    'Estado operativo actual del consultorio';


-- =========================================================
-- PROPIETARIO DE LAS TABLAS
-- =========================================================

ALTER TABLE soracare.sedes
    OWNER TO soracare_app;

ALTER TABLE soracare.consultorios
    OWNER TO soracare_app;

COMMIT;