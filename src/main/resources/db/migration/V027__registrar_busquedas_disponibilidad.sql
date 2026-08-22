BEGIN;

-- Registra demanda de agenda sin almacenar identidad, IP ni datos clínicos.
CREATE TABLE IF NOT EXISTS soracare.busquedas_disponibilidad (
    id_busqueda BIGINT GENERATED ALWAYS AS IDENTITY,
    id_medico BIGINT,
    id_especialidad SMALLINT,
    fecha_consultada DATE NOT NULL,
    cantidad_turnos SMALLINT NOT NULL DEFAULT 0,
    origen VARCHAR(20) NOT NULL,
    fecha_busqueda TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_busquedas_disponibilidad PRIMARY KEY (id_busqueda),
    CONSTRAINT fk_busqueda_disponibilidad_medico
        FOREIGN KEY (id_medico)
            REFERENCES soracare.medicos (id_medico)
            ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT fk_busqueda_disponibilidad_especialidad
        FOREIGN KEY (id_especialidad)
            REFERENCES soracare.especialidades (id_especialidad)
            ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT ck_busqueda_disponibilidad_turnos
        CHECK (cantidad_turnos >= 0),
    CONSTRAINT ck_busqueda_disponibilidad_origen
        CHECK (origen IN ('PUBLICO', 'PACIENTE'))
);

CREATE INDEX IF NOT EXISTS ix_busquedas_disponibilidad_fecha
    ON soracare.busquedas_disponibilidad (fecha_consultada);

CREATE INDEX IF NOT EXISTS ix_busquedas_disponibilidad_medico_fecha
    ON soracare.busquedas_disponibilidad (id_medico, fecha_consultada);

CREATE INDEX IF NOT EXISTS ix_busquedas_disponibilidad_especialidad_fecha
    ON soracare.busquedas_disponibilidad (id_especialidad, fecha_consultada);

COMMENT ON TABLE soracare.busquedas_disponibilidad IS
    'Consultas anonimas de turnos utilizadas para analizar la demanda de agenda';

COMMENT ON COLUMN soracare.busquedas_disponibilidad.fecha_consultada IS
    'Fecha de atencion que la persona consulto, no la fecha de la busqueda';

ALTER TABLE soracare.busquedas_disponibilidad OWNER TO soracare_app;

COMMIT;
