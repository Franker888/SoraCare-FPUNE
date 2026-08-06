CREATE SEQUENCE soracare.numero_historia_clinica_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1;

ALTER SEQUENCE soracare.numero_historia_clinica_seq
    OWNER TO soracare_app;

CREATE TABLE soracare.pacientes (
                                    id_paciente BIGINT
                                        GENERATED ALWAYS AS IDENTITY,

                                    id_persona BIGINT NOT NULL,

                                    numero_historia_clinica VARCHAR(20)
                                                      NOT NULL DEFAULT (
                                        'HC-' ||
                                        LPAD(
                                                NEXTVAL(
                                                        'soracare.numero_historia_clinica_seq'
                                                )::TEXT,
                                                8,
                                                '0'
                                        )
                                        ),

                                    grupo_sanguineo VARCHAR(5),

                                    contacto_emergencia VARCHAR(150),
                                    telefono_emergencia VARCHAR(30),

                                    creado_en TIMESTAMP WITH TIME ZONE
                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    actualizado_en TIMESTAMP WITH TIME ZONE
                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT pk_pacientes
                                        PRIMARY KEY (id_paciente),

                                    CONSTRAINT uq_pacientes_persona
                                        UNIQUE (id_persona),

                                    CONSTRAINT uq_pacientes_historia_clinica
                                        UNIQUE (numero_historia_clinica),

                                    CONSTRAINT fk_pacientes_personas
                                        FOREIGN KEY (id_persona)
                                            REFERENCES soracare.personas (id_persona)
                                            ON UPDATE CASCADE
                                            ON DELETE RESTRICT,

                                    CONSTRAINT ck_pacientes_grupo_sanguineo
                                        CHECK (
                                            grupo_sanguineo IS NULL
                                                OR grupo_sanguineo IN (
                                                                       'A+', 'A-',
                                                                       'B+', 'B-',
                                                                       'AB+', 'AB-',
                                                                       'O+', 'O-'
                                                )
                                            ),

                                    CONSTRAINT ck_pacientes_contacto_emergencia
                                        CHECK (
                                            contacto_emergencia IS NULL
                                                OR BTRIM(contacto_emergencia) <> ''
                                            ),

                                    CONSTRAINT ck_pacientes_telefono_emergencia
                                        CHECK (
                                            telefono_emergencia IS NULL
                                                OR BTRIM(telefono_emergencia) <> ''
                                            )
);

COMMENT ON TABLE soracare.pacientes IS
    'Información específica de los pacientes del sistema';

ALTER TABLE soracare.pacientes
    OWNER TO soracare_app;