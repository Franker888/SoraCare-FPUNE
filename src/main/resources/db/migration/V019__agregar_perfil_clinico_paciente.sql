BEGIN;

ALTER TABLE soracare.pacientes
    ADD COLUMN IF NOT EXISTS antecedentes_patologicos_personales TEXT,
    ADD COLUMN IF NOT EXISTS farmacos_utilizados TEXT,
    ADD COLUMN IF NOT EXISTS alergias TEXT;

COMMENT ON COLUMN soracare.pacientes.antecedentes_patologicos_personales IS
    'Antecedentes patológicos personales vigentes del paciente.';

COMMENT ON COLUMN soracare.pacientes.farmacos_utilizados IS
    'Medicamentos utilizados o de uso habitual informados por el paciente.';

COMMENT ON COLUMN soracare.pacientes.alergias IS
    'Alergias conocidas del paciente, incluidas las medicamentosas.';

COMMIT;
