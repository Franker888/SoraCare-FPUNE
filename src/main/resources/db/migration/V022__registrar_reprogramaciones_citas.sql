CREATE TABLE soracare.reprogramaciones_citas (
    id_reprogramacion BIGINT GENERATED ALWAYS AS IDENTITY,
    id_cita BIGINT NOT NULL,
    fecha_hora_inicio_anterior TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_hora_fin_anterior TIMESTAMP WITH TIME ZONE NOT NULL,
    id_medico_anterior BIGINT NOT NULL,
    id_especialidad_anterior SMALLINT NOT NULL,
    id_consultorio_anterior BIGINT NOT NULL,
    id_horario_anterior BIGINT,
    fecha_hora_inicio_nueva TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_hora_fin_nueva TIMESTAMP WITH TIME ZONE NOT NULL,
    id_medico_nuevo BIGINT NOT NULL,
    id_especialidad_nueva SMALLINT NOT NULL,
    id_consultorio_nuevo BIGINT NOT NULL,
    id_horario_nuevo BIGINT,
    motivo VARCHAR(500) NOT NULL,
    id_usuario BIGINT NOT NULL,
    fecha_reprogramacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_reprogramaciones_citas
        PRIMARY KEY (id_reprogramacion),
    CONSTRAINT fk_reprogramacion_cita
        FOREIGN KEY (id_cita)
            REFERENCES soracare.citas (id_cita)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_reprogramacion_medico_especialidad_anterior
        FOREIGN KEY (id_medico_anterior, id_especialidad_anterior)
            REFERENCES soracare.medicos_especialidades (id_medico, id_especialidad)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_reprogramacion_medico_especialidad_nueva
        FOREIGN KEY (id_medico_nuevo, id_especialidad_nueva)
            REFERENCES soracare.medicos_especialidades (id_medico, id_especialidad)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_reprogramacion_consultorio_anterior
        FOREIGN KEY (id_consultorio_anterior)
            REFERENCES soracare.consultorios (id_consultorio)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_reprogramacion_consultorio_nuevo
        FOREIGN KEY (id_consultorio_nuevo)
            REFERENCES soracare.consultorios (id_consultorio)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_reprogramacion_horario_anterior
        FOREIGN KEY (id_horario_anterior)
            REFERENCES soracare.horarios_medicos (id_horario)
            ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT fk_reprogramacion_horario_nuevo
        FOREIGN KEY (id_horario_nuevo)
            REFERENCES soracare.horarios_medicos (id_horario)
            ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT fk_reprogramacion_usuario
        FOREIGN KEY (id_usuario)
            REFERENCES soracare.usuarios (id_usuario)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_reprogramacion_horas_anteriores
        CHECK (fecha_hora_inicio_anterior < fecha_hora_fin_anterior),
    CONSTRAINT chk_reprogramacion_horas_nuevas
        CHECK (fecha_hora_inicio_nueva < fecha_hora_fin_nueva),
    CONSTRAINT chk_reprogramacion_cambio_fecha
        CHECK (fecha_hora_inicio_anterior <> fecha_hora_inicio_nueva),
    CONSTRAINT chk_reprogramacion_motivo
        CHECK (BTRIM(motivo) <> '')
);

CREATE INDEX idx_reprogramaciones_cita_fecha
    ON soracare.reprogramaciones_citas (id_cita, fecha_reprogramacion DESC);

COMMENT ON TABLE soracare.reprogramaciones_citas IS
    'Historial inmutable de cambios de turno realizados sobre una misma cita';

COMMENT ON COLUMN soracare.reprogramaciones_citas.motivo IS
    'Motivo informado por el usuario al cambiar la fecha u hora de la cita';

ALTER TABLE soracare.reprogramaciones_citas OWNER TO soracare_app;
