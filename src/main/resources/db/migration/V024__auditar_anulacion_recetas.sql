BEGIN;

ALTER TABLE soracare.recetas_medicas
    ADD COLUMN IF NOT EXISTS fecha_anulacion TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS motivo_anulacion VARCHAR(500),
    ADD COLUMN IF NOT EXISTS id_usuario_anulacion BIGINT;

ALTER TABLE soracare.recetas_medicas
    DROP CONSTRAINT IF EXISTS fk_recetas_usuario_anulacion;

ALTER TABLE soracare.recetas_medicas
    ADD CONSTRAINT fk_recetas_usuario_anulacion
        FOREIGN KEY (id_usuario_anulacion)
        REFERENCES soracare.usuarios (id_usuario)
        ON UPDATE CASCADE
        ON DELETE RESTRICT;

ALTER TABLE soracare.recetas_medicas
    DROP CONSTRAINT IF EXISTS ck_recetas_datos_anulacion;

ALTER TABLE soracare.recetas_medicas
    ADD CONSTRAINT ck_recetas_datos_anulacion CHECK (
        (estado = 'ANULADA' AND fecha_anulacion IS NOT NULL
            AND motivo_anulacion IS NOT NULL AND id_usuario_anulacion IS NOT NULL)
        OR
        (estado <> 'ANULADA' AND fecha_anulacion IS NULL
            AND motivo_anulacion IS NULL AND id_usuario_anulacion IS NULL)
    );

COMMENT ON COLUMN soracare.recetas_medicas.motivo_anulacion IS
    'Justificación obligatoria y auditable para anular una receta emitida.';

COMMIT;
