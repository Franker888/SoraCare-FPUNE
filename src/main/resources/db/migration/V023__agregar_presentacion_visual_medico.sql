ALTER TABLE soracare.medicos
    ADD COLUMN tratamiento VARCHAR(3) NOT NULL DEFAULT 'DR',
    ADD COLUMN foto_perfil VARCHAR(255),
    ADD CONSTRAINT chk_medicos_tratamiento
        CHECK (tratamiento IN ('DR', 'DRA')),
    ADD CONSTRAINT chk_medicos_foto_perfil
        CHECK (
            foto_perfil IS NULL
            OR foto_perfil ~ '^[A-Za-z0-9._-]+$'
        );

UPDATE soracare.medicos
   SET tratamiento = 'DR',
       foto_perfil = 'doctor-carlos-demo.png'
 WHERE matricula_profesional = 'MED-WEB-001';

COMMENT ON COLUMN soracare.medicos.tratamiento IS
    'Tratamiento profesional visible del médico: Dr. o Dra.';

COMMENT ON COLUMN soracare.medicos.foto_perfil IS
    'Nombre seguro del archivo de fotografía mostrado en el catálogo público';
