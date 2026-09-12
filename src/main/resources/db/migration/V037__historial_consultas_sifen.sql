CREATE TABLE soracare.consultas_sifen (
    id_consulta BIGINT PRIMARY KEY CHECK (id_consulta > 0),
    id_solicitud BIGINT NOT NULL REFERENCES soracare.solicitudes_sifen(id_solicitud),
    id_persona_medico BIGINT NOT NULL REFERENCES soracare.personas(id_persona),
    cdc VARCHAR(44) NOT NULL CHECK (cdc ~ '^[0-9]{44}$'),
    ambiente VARCHAR(12) NOT NULL DEFAULT 'PRUEBA' CHECK (ambiente = 'PRUEBA'),
    iniciado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    terminado_en TIMESTAMPTZ,
    estado VARCHAR(20) NOT NULL DEFAULT 'SIN_RESULTADO'
        CHECK (estado IN ('SIN_RESULTADO', 'ENCONTRADO', 'NO_ENCONTRADO', 'SIN_PERMISO', 'ERROR')),
    codigo VARCHAR(40),
    mensaje VARCHAR(1000),
    protocolo VARCHAR(80),
    eventos INTEGER CHECK (eventos >= 0),
    CHECK ((estado = 'SIN_RESULTADO' AND terminado_en IS NULL)
        OR (estado <> 'SIN_RESULTADO' AND terminado_en IS NOT NULL AND codigo IS NOT NULL AND mensaje IS NOT NULL))
);
CREATE INDEX idx_consultas_sifen_medico ON soracare.consultas_sifen(id_persona_medico, id_consulta DESC);
CREATE INDEX idx_consultas_sifen_solicitud ON soracare.consultas_sifen(id_solicitud, id_consulta DESC);
ALTER TABLE soracare.consultas_sifen OWNER TO soracare_app;
