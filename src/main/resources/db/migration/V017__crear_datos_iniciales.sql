BEGIN;

-- =========================================================
-- ESPECIALIDADES INICIALES
-- =========================================================

INSERT INTO soracare.especialidades (nombre)
SELECT datos.nombre
FROM (
         VALUES
             ('Clínica Médica'),
             ('Cardiología'),
             ('Pediatría'),
             ('Ginecología y Obstetricia'),
             ('Dermatología'),
             ('Traumatología y Ortopedia'),
             ('Neurología'),
             ('Psicología')
     ) AS datos(nombre)
WHERE NOT EXISTS (
    SELECT 1
    FROM soracare.especialidades especialidad
    WHERE LOWER(TRIM(especialidad.nombre))
              = LOWER(TRIM(datos.nombre))
);

-- =========================================================
-- SEDE INICIAL
-- =========================================================

INSERT INTO soracare.sedes (
    codigo,
    nombre,
    direccion,
    ciudad
)
SELECT
    'SEDE-CENTRAL',
    'Sede Central',
    'Dirección institucional pendiente de confirmación',
    'Ciudad del Este'
    WHERE NOT EXISTS (
    SELECT 1
    FROM soracare.sedes sede
    WHERE sede.codigo = 'SEDE-CENTRAL'
);

-- =========================================================
-- CONSULTORIOS INICIALES
-- =========================================================

INSERT INTO soracare.consultorios (
    id_sede,
    codigo,
    nombre
)
SELECT
    sede.id_sede,
    datos.codigo,
    datos.nombre
FROM soracare.sedes sede
         CROSS JOIN (
    VALUES
        ('CONS-001', 'Consultorio 01'),
        ('CONS-002', 'Consultorio 02'),
        ('CONS-003', 'Consultorio 03'),
        ('CONS-004', 'Consultorio 04'),
        ('CONS-005', 'Consultorio 05'),
        ('CONS-006', 'Consultorio 06')
) AS datos(codigo, nombre)
WHERE sede.codigo = 'SEDE-CENTRAL'
  AND NOT EXISTS (
    SELECT 1
    FROM soracare.consultorios consultorio
    WHERE consultorio.id_sede = sede.id_sede
      AND consultorio.codigo = datos.codigo
);

COMMIT;