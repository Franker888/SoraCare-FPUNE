\set ON_ERROR_STOP on

BEGIN;

CREATE TEMP TABLE datos_demo_medicos (
    cedula BIGINT PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    telefono VARCHAR(30) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    matricula VARCHAR(50) NOT NULL,
    tratamiento VARCHAR(3) NOT NULL,
    biografia VARCHAR(1000) NOT NULL,
    foto VARCHAR(255) NOT NULL,
    especialidad VARCHAR(100) NOT NULL,
    consultorio VARCHAR(30) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    usuario VARCHAR(50) NOT NULL,
    avatar VARCHAR(20) NOT NULL,
    dias SMALLINT[] NOT NULL
) ON COMMIT DROP;

INSERT INTO datos_demo_medicos VALUES
    (910000001, 'Valeria', 'Benítez', DATE '1987-04-12', '0981 410 001',
     'valeria.benitez@example.invalid', 'Barrio Boquerón, Ciudad del Este',
     'DEMO-DER-001', 'DRA',
     'Especialista en dermatología clínica, prevención y cuidado integral de la piel.',
     'doctor1.jpg', 'Dermatología', 'CONS-001', TIME '08:00', TIME '12:00',
     'med.derma.valeria', 'TURQUESA', ARRAY[1, 3, 5]::SMALLINT[]),
    (910000002, 'Diego', 'Ferreira', DATE '1984-09-25', '0981 410 002',
     'diego.ferreira@example.invalid', 'Área 1, Ciudad del Este',
     'DEMO-DER-002', 'DR',
     'Médico dermatólogo orientado al diagnóstico temprano y tratamiento personalizado.',
     'doctor-carlos-demo.png', 'Dermatología', 'CONS-001', TIME '08:00', TIME '12:00',
     'med.derma.diego', 'AZUL', ARRAY[2, 4, 6]::SMALLINT[]),

    (910000003, 'Lucía', 'Gómez', DATE '1988-02-17', '0981 420 001',
     'lucia.gomez@example.invalid', 'Barrio San José, Ciudad del Este',
     'DEMO-PSI-001', 'DRA',
     'Psicóloga clínica con enfoque en bienestar emocional, ansiedad y acompañamiento familiar.',
     'doctor1.jpg', 'Psicología', 'CONS-002', TIME '08:00', TIME '12:00',
     'med.psico.lucia', 'VIOLETA', ARRAY[1, 3, 5]::SMALLINT[]),
    (910000004, 'Miguel', 'Rojas', DATE '1983-11-08', '0981 420 002',
     'miguel.rojas@example.invalid', 'Barrio Pablo Rojas, Ciudad del Este',
     'DEMO-PSI-002', 'DR',
     'Profesional de salud mental dedicado a la atención de adultos y manejo del estrés.',
     'doctor-carlos-demo.png', 'Psicología', 'CONS-002', TIME '08:00', TIME '12:00',
     'med.psico.miguel', 'VERDE', ARRAY[2, 4, 6]::SMALLINT[]),

    (910000005, 'Ana', 'Martínez', DATE '1985-06-30', '0981 430 001',
     'ana.martinez@example.invalid', 'Barrio Don Bosco, Ciudad del Este',
     'DEMO-CLI-001', 'DRA',
     'Especialista en clínica médica, prevención de enfermedades y seguimiento integral del adulto.',
     'doctor1.jpg', 'Clínica Médica', 'CONS-003', TIME '08:00', TIME '12:00',
     'med.clinica.ana', 'CORAL', ARRAY[1, 3, 5]::SMALLINT[]),
    (910000006, 'Carlos', 'Ramírez', DATE '1981-01-14', '0981 430 002',
     'carlos.ramirez@example.invalid', 'Barrio Obrero, Ciudad del Este',
     'DEMO-CLI-002', 'DR',
     'Médico clínico enfocado en diagnóstico, control de enfermedades crónicas y medicina preventiva.',
     'doctor-carlos-demo.png', 'Clínica Médica', 'CONS-003', TIME '08:00', TIME '12:00',
     'med.clinica.carlos', 'DORADO', ARRAY[2, 4, 6]::SMALLINT[]),

    (910000007, 'Sofía', 'López', DATE '1990-03-21', '0981 440 001',
     'sofia.lopez@example.invalid', 'Barrio Ciudad Nueva, Ciudad del Este',
     'DEMO-PED-001', 'DRA',
     'Pediatra dedicada al crecimiento, desarrollo y cuidado preventivo de niños y adolescentes.',
     'doctor3.jpg', 'Pediatría', 'CONS-004', TIME '08:00', TIME '12:00',
     'med.pediatria.sofia', 'TURQUESA', ARRAY[1, 3, 5]::SMALLINT[]),
    (910000008, 'Javier', 'Acosta', DATE '1986-12-04', '0981 440 002',
     'javier.acosta@example.invalid', 'Barrio Remansito, Ciudad del Este',
     'DEMO-PED-002', 'DR',
     'Especialista en pediatría general y orientación familiar para una infancia saludable.',
     'doctor3.jpg', 'Pediatría', 'CONS-004', TIME '08:00', TIME '12:00',
     'med.pediatria.javier', 'AZUL', ARRAY[2, 4, 6]::SMALLINT[]),

    (910000009, 'Mariana', 'González', DATE '1984-07-19', '0981 450 001',
     'mariana.gonzalez@example.invalid', 'Barrio Fátima, Presidente Franco',
     'DEMO-TRA-001', 'DRA',
     'Traumatóloga especializada en lesiones musculoesqueléticas y recuperación funcional.',
     'doctor1.jpg', 'Traumatología y Ortopedia', 'CONS-005', TIME '08:00', TIME '12:00',
     'med.trauma.mariana', 'VIOLETA', ARRAY[1, 3, 5]::SMALLINT[]),
    (910000010, 'Roberto', 'Villalba', DATE '1979-10-11', '0981 450 002',
     'roberto.villalba@example.invalid', 'Área 5, Presidente Franco',
     'DEMO-TRA-002', 'DR',
     'Ortopedista con experiencia en evaluación de lesiones, articulaciones y rehabilitación.',
     'doctor-carlos-demo.png', 'Traumatología y Ortopedia', 'CONS-005', TIME '08:00', TIME '12:00',
     'med.trauma.roberto', 'VERDE', ARRAY[2, 4, 6]::SMALLINT[]),

    (910000011, 'Elena', 'Duarte', DATE '1982-05-28', '0981 460 001',
     'elena.duarte@example.invalid', 'Barrio San Juan, Hernandarias',
     'DEMO-CAR-001', 'DRA',
     'Cardióloga enfocada en prevención cardiovascular y control integral de factores de riesgo.',
     'doctor1.jpg', 'Cardiología', 'CONS-006', TIME '08:00', TIME '12:00',
     'med.cardio.elena', 'CORAL', ARRAY[1, 3, 5]::SMALLINT[]),
    (910000012, 'Fernando', 'Núñez', DATE '1980-08-16', '0981 460 002',
     'fernando.nunez@example.invalid', 'Centro, Hernandarias',
     'DEMO-CAR-002', 'DR',
     'Especialista en cardiología clínica y seguimiento de pacientes con enfermedades crónicas.',
     'doctor-carlos-demo.png', 'Cardiología', 'CONS-006', TIME '08:00', TIME '12:00',
     'med.cardio.fernando', 'DORADO', ARRAY[2, 4, 6]::SMALLINT[]),

    (910000013, 'Camila', 'Ortiz', DATE '1989-01-09', '0981 470 001',
     'camila.ortiz@example.invalid', 'Barrio Boquerón, Ciudad del Este',
     'DEMO-NEU-001', 'DRA',
     'Neuróloga dedicada al diagnóstico y seguimiento de trastornos del sistema nervioso.',
     'doctor2.jpg', 'Neurología', 'CONS-001', TIME '13:00', TIME '17:00',
     'med.neuro.camila', 'TURQUESA', ARRAY[1, 3, 5]::SMALLINT[]),
    (910000014, 'Andrés', 'Caballero', DATE '1983-04-23', '0981 470 002',
     'andres.caballero@example.invalid', 'Área 2, Ciudad del Este',
     'DEMO-NEU-002', 'DR',
     'Médico neurólogo con experiencia en cefaleas, memoria y evaluación neurológica integral.',
     'doctor2.jpg', 'Neurología', 'CONS-001', TIME '13:00', TIME '17:00',
     'med.neuro.andres', 'AZUL', ARRAY[2, 4, 6]::SMALLINT[]),

    (910000015, 'Patricia', 'Vera', DATE '1985-09-02', '0981 480 001',
     'patricia.vera@example.invalid', 'Barrio San Miguel, Ciudad del Este',
     'DEMO-GIN-001', 'DRA',
     'Especialista en salud integral de la mujer, control ginecológico y obstetricia.',
     'doctor1.jpg', 'Ginecología y Obstetricia', 'CONS-002', TIME '13:00', TIME '17:00',
     'med.gine.patricia', 'VIOLETA', ARRAY[1, 3, 5]::SMALLINT[]),
    (910000016, 'Gabriela', 'Franco', DATE '1987-12-13', '0981 480 002',
     'gabriela.franco@example.invalid', 'Barrio Santa Ana, Ciudad del Este',
     'DEMO-GIN-002', 'DRA',
     'Ginecóloga y obstetra orientada a la prevención, maternidad segura y atención personalizada.',
     'doctor3.jpg', 'Ginecología y Obstetricia', 'CONS-002', TIME '13:00', TIME '17:00',
     'med.gine.gabriela', 'CORAL', ARRAY[2, 4, 6]::SMALLINT[]);

CREATE TEMP TABLE datos_demo_cuentas (
    cedula BIGINT PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    telefono VARCHAR(30) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    usuario VARCHAR(50) NOT NULL,
    rol VARCHAR(30) NOT NULL,
    avatar VARCHAR(20) NOT NULL,
    contrasena_hash VARCHAR(255) NOT NULL
) ON COMMIT DROP;

INSERT INTO datos_demo_cuentas VALUES
    (920000001, 'Andrea', 'Administradora Demo', DATE '1988-05-15',
     '0981 500 001', 'admin.soracare@example.invalid',
     'Sede Central, Ciudad del Este', 'prueba.seguridad',
     'ADMINISTRADOR', 'DORADO', :'hash_admin'),
    (920000002, 'Rocío', 'Recepción Demo', DATE '1993-08-10',
     '0981 500 002', 'recepcion.soracare@example.invalid',
     'Sede Central, Ciudad del Este', 'prueba.recepcion',
     'RECEPCIONISTA', 'TURQUESA', :'hash_recepcion'),
    (920000003, 'Adrián', 'Paciente de Prueba', DATE '2000-01-20',
     '0981 500 003', 'adrian.paciente@example.invalid',
     'Ciudad del Este, Alto Paraná', 'adrian.paciente',
     'PACIENTE', 'AZUL', :'hash_paciente');

INSERT INTO soracare.personas (
    nombres, apellidos, cedula, fecha_nacimiento,
    telefono, correo, direccion, activo
)
SELECT nombres, apellidos, cedula, fecha_nacimiento,
       telefono, correo, direccion, TRUE
FROM (
    SELECT nombres, apellidos, cedula, fecha_nacimiento,
           telefono, correo, direccion
      FROM datos_demo_medicos
    UNION ALL
    SELECT nombres, apellidos, cedula, fecha_nacimiento,
           telefono, correo, direccion
      FROM datos_demo_cuentas
) AS personas_demo
ON CONFLICT (cedula) DO UPDATE
SET nombres = EXCLUDED.nombres,
    apellidos = EXCLUDED.apellidos,
    fecha_nacimiento = EXCLUDED.fecha_nacimiento,
    telefono = EXCLUDED.telefono,
    correo = EXCLUDED.correo,
    direccion = EXCLUDED.direccion,
    activo = TRUE,
    actualizado_en = CURRENT_TIMESTAMP;

INSERT INTO soracare.medicos (
    id_persona, matricula_profesional, fecha_ingreso,
    estado, biografia, tratamiento, foto_perfil
)
SELECT persona.id_persona, datos.matricula, DATE '2024-01-15',
       'ACTIVO', datos.biografia, datos.tratamiento, datos.foto
  FROM datos_demo_medicos datos
  JOIN soracare.personas persona ON persona.cedula = datos.cedula
ON CONFLICT (id_persona) DO UPDATE
SET matricula_profesional = EXCLUDED.matricula_profesional,
    fecha_ingreso = EXCLUDED.fecha_ingreso,
    estado = 'ACTIVO',
    biografia = EXCLUDED.biografia,
    tratamiento = EXCLUDED.tratamiento,
    foto_perfil = EXCLUDED.foto_perfil,
    fecha_actualizacion = CURRENT_TIMESTAMP;

INSERT INTO soracare.medicos_especialidades (
    id_medico, id_especialidad, es_principal,
    fecha_desde, fecha_hasta, activo
)
SELECT medico.id_medico, especialidad.id_especialidad, TRUE,
       DATE '2024-01-15', NULL, TRUE
  FROM datos_demo_medicos datos
  JOIN soracare.personas persona ON persona.cedula = datos.cedula
  JOIN soracare.medicos medico ON medico.id_persona = persona.id_persona
  JOIN soracare.especialidades especialidad
    ON LOWER(BTRIM(especialidad.nombre)) = LOWER(BTRIM(datos.especialidad))
ON CONFLICT (id_medico, id_especialidad) DO UPDATE
SET es_principal = TRUE,
    fecha_desde = EXCLUDED.fecha_desde,
    fecha_hasta = NULL,
    activo = TRUE;

INSERT INTO soracare.pacientes (
    id_persona, grupo_sanguineo,
    contacto_emergencia, telefono_emergencia,
    antecedentes_patologicos_personales,
    farmacos_utilizados, alergias
)
SELECT persona.id_persona, 'O+',
       'Contacto de prueba', '0981 599 999',
       'Sin antecedentes relevantes registrados.',
       'No utiliza medicación habitual.',
       'Sin alergias conocidas.'
  FROM datos_demo_cuentas datos
  JOIN soracare.personas persona ON persona.cedula = datos.cedula
 WHERE datos.rol = 'PACIENTE'
ON CONFLICT (id_persona) DO UPDATE
SET grupo_sanguineo = EXCLUDED.grupo_sanguineo,
    contacto_emergencia = EXCLUDED.contacto_emergencia,
    telefono_emergencia = EXCLUDED.telefono_emergencia,
    antecedentes_patologicos_personales = EXCLUDED.antecedentes_patologicos_personales,
    farmacos_utilizados = EXCLUDED.farmacos_utilizados,
    alergias = EXCLUDED.alergias,
    actualizado_en = CURRENT_TIMESTAMP;

WITH cuentas AS (
    SELECT cedula, usuario, 'MEDICO'::VARCHAR(30) AS rol,
           avatar, :'hash_medicos'::VARCHAR(255) AS contrasena_hash
      FROM datos_demo_medicos
    UNION ALL
    SELECT cedula, usuario, rol, avatar, contrasena_hash
      FROM datos_demo_cuentas
)
INSERT INTO soracare.usuarios (
    id_persona, nombre_usuario, contrasena_hash,
    estado, intentos_fallidos, bloqueado_hasta,
    debe_cambiar_contrasena
)
SELECT persona.id_persona, cuenta.usuario, cuenta.contrasena_hash,
       'ACTIVO', 0, NULL, FALSE
  FROM cuentas cuenta
  JOIN soracare.personas persona ON persona.cedula = cuenta.cedula
ON CONFLICT (id_persona) DO UPDATE
SET nombre_usuario = EXCLUDED.nombre_usuario,
    contrasena_hash = EXCLUDED.contrasena_hash,
    estado = 'ACTIVO',
    intentos_fallidos = 0,
    bloqueado_hasta = NULL,
    debe_cambiar_contrasena = FALSE,
    actualizado_en = CURRENT_TIMESTAMP;

WITH cuentas AS (
    SELECT usuario, 'MEDICO'::VARCHAR(30) AS rol
      FROM datos_demo_medicos
    UNION ALL
    SELECT usuario, rol FROM datos_demo_cuentas
)
INSERT INTO soracare.usuarios_roles (id_usuario, id_rol)
SELECT usuario.id_usuario, rol.id_rol
  FROM cuentas cuenta
  JOIN soracare.usuarios usuario
    ON LOWER(usuario.nombre_usuario) = LOWER(cuenta.usuario)
  JOIN soracare.roles rol ON rol.nombre = cuenta.rol
ON CONFLICT (id_usuario, id_rol) DO NOTHING;

WITH perfiles AS (
    SELECT cedula, avatar FROM datos_demo_medicos
    UNION ALL
    SELECT cedula, avatar FROM datos_demo_cuentas
)
INSERT INTO soracare.perfiles_visuales (id_persona, avatar_codigo)
SELECT persona.id_persona, perfil.avatar
  FROM perfiles perfil
  JOIN soracare.personas persona ON persona.cedula = perfil.cedula
ON CONFLICT (id_persona) DO UPDATE
SET avatar_codigo = EXCLUDED.avatar_codigo,
    fecha_actualizacion = CURRENT_TIMESTAMP;

INSERT INTO soracare.horarios_medicos (
    id_medico, id_especialidad, id_consultorio,
    dia_semana, hora_inicio, hora_fin,
    duracion_turno_minutos, fecha_desde,
    fecha_hasta, activo
)
SELECT medico.id_medico,
       especialidad.id_especialidad,
       consultorio.id_consultorio,
       dia.dia_semana,
       datos.hora_inicio,
       datos.hora_fin,
       especialidad.duracion_consulta_minutos,
       DATE '2026-01-01',
       NULL,
       TRUE
  FROM datos_demo_medicos datos
  JOIN soracare.personas persona ON persona.cedula = datos.cedula
  JOIN soracare.medicos medico ON medico.id_persona = persona.id_persona
  JOIN soracare.especialidades especialidad
    ON LOWER(BTRIM(especialidad.nombre)) = LOWER(BTRIM(datos.especialidad))
  JOIN soracare.consultorios consultorio
    ON consultorio.codigo = datos.consultorio
 CROSS JOIN LATERAL UNNEST(datos.dias) AS dia(dia_semana)
ON CONFLICT (
    id_medico, id_especialidad, id_consultorio,
    dia_semana, hora_inicio, fecha_desde
) DO UPDATE
SET hora_fin = EXCLUDED.hora_fin,
    duracion_turno_minutos = EXCLUDED.duracion_turno_minutos,
    fecha_hasta = NULL,
    activo = TRUE,
    fecha_actualizacion = CURRENT_TIMESTAMP;

DO $$
DECLARE
    cantidad_medicos INTEGER;
    cantidad_especialidades INTEGER;
    cantidad_horarios INTEGER;
    cantidad_cuentas INTEGER;
BEGIN
    SELECT COUNT(*) INTO cantidad_medicos
      FROM soracare.medicos
     WHERE matricula_profesional LIKE 'DEMO-%';

    SELECT COUNT(*) INTO cantidad_especialidades
      FROM (
          SELECT especialidad.id_especialidad
            FROM soracare.especialidades especialidad
            JOIN soracare.medicos_especialidades relacion
              ON relacion.id_especialidad = especialidad.id_especialidad
            JOIN soracare.medicos medico
              ON medico.id_medico = relacion.id_medico
           WHERE medico.matricula_profesional LIKE 'DEMO-%'
             AND relacion.activo = TRUE
           GROUP BY especialidad.id_especialidad
          HAVING COUNT(*) = 2
      ) AS especialidades_completas;

    SELECT COUNT(*) INTO cantidad_horarios
      FROM soracare.horarios_medicos horario
      JOIN soracare.medicos medico ON medico.id_medico = horario.id_medico
     WHERE medico.matricula_profesional LIKE 'DEMO-%'
       AND horario.activo = TRUE;

    SELECT COUNT(*) INTO cantidad_cuentas
      FROM soracare.usuarios usuario
      JOIN soracare.personas persona ON persona.id_persona = usuario.id_persona
     WHERE persona.cedula BETWEEN 910000001 AND 920000003;

    IF cantidad_medicos <> 16
       OR cantidad_especialidades <> 8
       OR cantidad_horarios <> 48
       OR cantidad_cuentas <> 19 THEN
        RAISE EXCEPTION
            'Carga incompleta: medicos %, especialidades %, horarios %, cuentas %',
            cantidad_medicos, cantidad_especialidades,
            cantidad_horarios, cantidad_cuentas;
    END IF;
END
$$;

COMMIT;

SELECT 'Médicos creados' AS concepto, COUNT(*)::TEXT AS cantidad
  FROM soracare.medicos
 WHERE matricula_profesional LIKE 'DEMO-%'
UNION ALL
SELECT 'Especialidades con dos médicos', COUNT(*)::TEXT
  FROM (
      SELECT relacion.id_especialidad
        FROM soracare.medicos_especialidades relacion
        JOIN soracare.medicos medico ON medico.id_medico = relacion.id_medico
       WHERE medico.matricula_profesional LIKE 'DEMO-%'
         AND relacion.activo = TRUE
       GROUP BY relacion.id_especialidad
      HAVING COUNT(*) = 2
  ) AS completas
UNION ALL
SELECT 'Horarios médicos activos', COUNT(*)::TEXT
  FROM soracare.horarios_medicos horario
  JOIN soracare.medicos medico ON medico.id_medico = horario.id_medico
 WHERE medico.matricula_profesional LIKE 'DEMO-%'
   AND horario.activo = TRUE
UNION ALL
SELECT 'Cuentas de prueba activas', COUNT(*)::TEXT
  FROM soracare.usuarios usuario
  JOIN soracare.personas persona ON persona.id_persona = usuario.id_persona
 WHERE persona.cedula BETWEEN 910000001 AND 920000003;
