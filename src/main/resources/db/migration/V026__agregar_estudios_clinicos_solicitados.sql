BEGIN;

ALTER TABLE soracare.atenciones_medicas
    ADD COLUMN IF NOT EXISTS estudios_clinicos_solicitados TEXT;

COMMENT ON COLUMN soracare.atenciones_medicas.estudios_clinicos_solicitados IS
    'Estudios de laboratorio, imágenes u otros procedimientos solicitados durante la atención.';

COMMIT;
