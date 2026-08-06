CREATE TABLE soracare.personas (
                                   id_persona BIGINT
                                       GENERATED ALWAYS AS IDENTITY,

                                   nombres VARCHAR(100) NOT NULL,
                                   apellidos VARCHAR(100) NOT NULL,
                                   cedula BIGINT NOT NULL,

                                   fecha_nacimiento DATE,
                                   telefono VARCHAR(30),
                                   correo VARCHAR(150),
                                   direccion VARCHAR(255),

                                   activo BOOLEAN
                                       NOT NULL DEFAULT TRUE,

                                   creado_en TIMESTAMP WITH TIME ZONE
                                       NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   actualizado_en TIMESTAMP WITH TIME ZONE
                                       NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT pk_personas
                                       PRIMARY KEY (id_persona),

                                   CONSTRAINT uq_personas_cedula
                                       UNIQUE (cedula),

                                   CONSTRAINT ck_personas_nombres
                                       CHECK (BTRIM(nombres) <> ''),

                                   CONSTRAINT ck_personas_apellidos
                                       CHECK (BTRIM(apellidos) <> ''),

                                   CONSTRAINT ck_personas_cedula
                                       CHECK (cedula > 0),

                                   CONSTRAINT ck_personas_correo
                                       CHECK (correo IS NULL OR BTRIM(correo) <> '')
);

CREATE UNIQUE INDEX uq_personas_correo_normalizado
    ON soracare.personas (LOWER(correo))
    WHERE correo IS NOT NULL;

COMMENT ON TABLE soracare.personas IS
    'Datos personales compartidos por pacientes, médicos y administradores';

ALTER TABLE soracare.personas
    OWNER TO soracare_app;