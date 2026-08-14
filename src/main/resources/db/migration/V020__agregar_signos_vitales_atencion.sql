BEGIN;

ALTER TABLE soracare.atenciones_medicas
    ADD COLUMN IF NOT EXISTS temperatura_c NUMERIC(4, 1),
    ADD COLUMN IF NOT EXISTS frecuencia_cardiaca SMALLINT,
    ADD COLUMN IF NOT EXISTS frecuencia_respiratoria SMALLINT,
    ADD COLUMN IF NOT EXISTS presion_sistolica SMALLINT,
    ADD COLUMN IF NOT EXISTS presion_diastolica SMALLINT,
    ADD COLUMN IF NOT EXISTS saturacion_oxigeno SMALLINT;

ALTER TABLE soracare.atenciones_medicas
    DROP CONSTRAINT IF EXISTS ck_atenciones_temperatura,
    ADD CONSTRAINT ck_atenciones_temperatura
        CHECK (temperatura_c IS NULL OR temperatura_c BETWEEN 30.0 AND 45.0),
    DROP CONSTRAINT IF EXISTS ck_atenciones_frecuencia_cardiaca,
    ADD CONSTRAINT ck_atenciones_frecuencia_cardiaca
        CHECK (frecuencia_cardiaca IS NULL OR frecuencia_cardiaca BETWEEN 20 AND 250),
    DROP CONSTRAINT IF EXISTS ck_atenciones_frecuencia_respiratoria,
    ADD CONSTRAINT ck_atenciones_frecuencia_respiratoria
        CHECK (frecuencia_respiratoria IS NULL OR frecuencia_respiratoria BETWEEN 5 AND 80),
    DROP CONSTRAINT IF EXISTS ck_atenciones_presion_sistolica,
    ADD CONSTRAINT ck_atenciones_presion_sistolica
        CHECK (presion_sistolica IS NULL OR presion_sistolica BETWEEN 50 AND 300),
    DROP CONSTRAINT IF EXISTS ck_atenciones_presion_diastolica,
    ADD CONSTRAINT ck_atenciones_presion_diastolica
        CHECK (presion_diastolica IS NULL OR presion_diastolica BETWEEN 30 AND 200),
    DROP CONSTRAINT IF EXISTS ck_atenciones_presion_arterial,
    ADD CONSTRAINT ck_atenciones_presion_arterial
        CHECK (
            presion_sistolica IS NULL
            OR presion_diastolica IS NULL
            OR presion_sistolica > presion_diastolica
        ),
    DROP CONSTRAINT IF EXISTS ck_atenciones_saturacion_oxigeno,
    ADD CONSTRAINT ck_atenciones_saturacion_oxigeno
        CHECK (saturacion_oxigeno IS NULL OR saturacion_oxigeno BETWEEN 0 AND 100);

COMMENT ON COLUMN soracare.atenciones_medicas.temperatura_c IS
    'Temperatura corporal en grados Celsius registrada durante la consulta.';
COMMENT ON COLUMN soracare.atenciones_medicas.frecuencia_cardiaca IS
    'Frecuencia cardíaca en latidos por minuto.';
COMMENT ON COLUMN soracare.atenciones_medicas.frecuencia_respiratoria IS
    'Frecuencia respiratoria en respiraciones por minuto.';
COMMENT ON COLUMN soracare.atenciones_medicas.presion_sistolica IS
    'Presión arterial sistólica en mmHg.';
COMMENT ON COLUMN soracare.atenciones_medicas.presion_diastolica IS
    'Presión arterial diastólica en mmHg.';
COMMENT ON COLUMN soracare.atenciones_medicas.saturacion_oxigeno IS
    'Saturación periférica de oxígeno expresada en porcentaje.';

COMMIT;
