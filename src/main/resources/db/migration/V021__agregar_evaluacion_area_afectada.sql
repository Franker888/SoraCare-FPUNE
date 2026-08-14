BEGIN;

ALTER TABLE soracare.atenciones_medicas
    ADD COLUMN IF NOT EXISTS evaluacion_area_afectada TEXT;

COMMENT ON COLUMN soracare.atenciones_medicas.evaluacion_area_afectada IS
    'Descripción de la inspección y evaluación clínica del área corporal afectada.';

COMMIT;
