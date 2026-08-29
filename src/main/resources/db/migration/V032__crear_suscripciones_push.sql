CREATE TABLE IF NOT EXISTS soracare.suscripciones_push (
    id_suscripcion BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    token TEXT NOT NULL,
    user_agent VARCHAR(500),
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultima_actividad_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_suscripciones_push_token UNIQUE (token),
    CONSTRAINT fk_suscripciones_push_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES soracare.usuarios (id_usuario)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_suscripciones_push_usuario_activa
    ON soracare.suscripciones_push (id_usuario, activa);
