BEGIN;

CREATE TABLE soracare.google_calendar_vinculos (
    id_usuario BIGINT PRIMARY KEY,
    correo_google VARCHAR(150) NOT NULL,
    refresh_token_cifrado TEXT NOT NULL,
    alcance VARCHAR(500) NOT NULL,
    vinculado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_google_calendar_vinculos_usuario
        FOREIGN KEY (id_usuario)
            REFERENCES soracare.usuarios (id_usuario)
            ON UPDATE CASCADE
            ON DELETE CASCADE,

    CONSTRAINT ck_google_calendar_vinculos_correo
        CHECK (BTRIM(correo_google) <> ''),

    CONSTRAINT ck_google_calendar_vinculos_token
        CHECK (BTRIM(refresh_token_cifrado) <> ''),

    CONSTRAINT ck_google_calendar_vinculos_alcance
        CHECK (BTRIM(alcance) <> '')
);

CREATE TABLE soracare.google_calendar_eventos (
    id_usuario BIGINT NOT NULL,
    id_cita BIGINT NOT NULL,
    id_evento_google VARCHAR(1024) NOT NULL,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_google_calendar_eventos
        PRIMARY KEY (id_usuario, id_cita),

    CONSTRAINT fk_google_calendar_eventos_usuario
        FOREIGN KEY (id_usuario)
            REFERENCES soracare.usuarios (id_usuario)
            ON UPDATE CASCADE
            ON DELETE CASCADE,

    CONSTRAINT fk_google_calendar_eventos_cita
        FOREIGN KEY (id_cita)
            REFERENCES soracare.citas (id_cita)
            ON UPDATE CASCADE
            ON DELETE CASCADE,

    CONSTRAINT uq_google_calendar_eventos_id_externo
        UNIQUE (id_usuario, id_evento_google),

    CONSTRAINT ck_google_calendar_eventos_id_externo
        CHECK (BTRIM(id_evento_google) <> '')
);

CREATE INDEX idx_google_calendar_eventos_cita
    ON soracare.google_calendar_eventos (id_cita);

COMMENT ON TABLE soracare.google_calendar_vinculos IS
    'Autorizaciones revocables de Google Calendar; el refresh token se cifra en la aplicacion';

COMMENT ON TABLE soracare.google_calendar_eventos IS
    'Relacion entre una cita de SoraCare y su evento en el calendario del paciente';

ALTER TABLE soracare.google_calendar_vinculos OWNER TO soracare_app;
ALTER TABLE soracare.google_calendar_eventos OWNER TO soracare_app;

COMMIT;
