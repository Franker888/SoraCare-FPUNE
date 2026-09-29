BEGIN;

INSERT INTO soracare.personas (
    nombres,
    apellidos,
    cedula,
    fecha_nacimiento,
    telefono,
    correo,
    direccion,
    activo
)
VALUES (
    'Adri',
    'Admin',
    920000004,
    NULL,
    NULL,
    'adri.admin@example.invalid',
    'Cuenta administrativa de demostracion',
    TRUE
)
ON CONFLICT (cedula) DO UPDATE
SET nombres = EXCLUDED.nombres,
    apellidos = EXCLUDED.apellidos,
    correo = EXCLUDED.correo,
    direccion = EXCLUDED.direccion,
    activo = TRUE,
    actualizado_en = CURRENT_TIMESTAMP;

DO $$
DECLARE
    persona_adri BIGINT;
BEGIN
    SELECT id_persona
      INTO persona_adri
      FROM soracare.personas
     WHERE cedula = 920000004;

    IF EXISTS (
        SELECT 1
          FROM soracare.usuarios
         WHERE LOWER(nombre_usuario) = 'adri.admin'
           AND id_persona <> persona_adri
    ) THEN
        RAISE EXCEPTION
            'El nombre de usuario adri.admin pertenece a otra persona.';
    END IF;
END;
$$;

INSERT INTO soracare.usuarios (
    id_persona,
    nombre_usuario,
    contrasena_hash,
    estado,
    intentos_fallidos,
    bloqueado_hasta,
    debe_cambiar_contrasena
)
SELECT persona.id_persona,
       'adri.admin',
       :'hash_admin',
       'ACTIVO',
       0,
       NULL,
       TRUE
  FROM soracare.personas persona
 WHERE persona.cedula = 920000004
ON CONFLICT (id_persona) DO UPDATE
SET nombre_usuario = EXCLUDED.nombre_usuario,
    contrasena_hash = EXCLUDED.contrasena_hash,
    estado = 'ACTIVO',
    intentos_fallidos = 0,
    bloqueado_hasta = NULL,
    debe_cambiar_contrasena = TRUE,
    actualizado_en = CURRENT_TIMESTAMP;

DELETE FROM soracare.usuarios_roles asignacion
USING soracare.usuarios usuario,
      soracare.personas persona,
      soracare.roles rol
WHERE asignacion.id_usuario = usuario.id_usuario
  AND usuario.id_persona = persona.id_persona
  AND persona.cedula = 920000004
  AND asignacion.id_rol = rol.id_rol
  AND rol.nombre <> 'ADMINISTRADOR';

INSERT INTO soracare.usuarios_roles (id_usuario, id_rol)
SELECT usuario.id_usuario, rol.id_rol
  FROM soracare.usuarios usuario
  JOIN soracare.personas persona
    ON persona.id_persona = usuario.id_persona
  JOIN soracare.roles rol
    ON rol.nombre = 'ADMINISTRADOR'
   AND rol.activo = TRUE
 WHERE persona.cedula = 920000004
ON CONFLICT (id_usuario, id_rol) DO NOTHING;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM soracare.usuarios usuario
          JOIN soracare.personas persona
            ON persona.id_persona = usuario.id_persona
          JOIN soracare.usuarios_roles asignacion
            ON asignacion.id_usuario = usuario.id_usuario
          JOIN soracare.roles rol
            ON rol.id_rol = asignacion.id_rol
         WHERE persona.cedula = 920000004
           AND persona.activo = TRUE
           AND LOWER(usuario.nombre_usuario) = 'adri.admin'
           AND usuario.estado = 'ACTIVO'
           AND usuario.debe_cambiar_contrasena = TRUE
           AND rol.nombre = 'ADMINISTRADOR'
           AND rol.activo = TRUE
    ) THEN
        RAISE EXCEPTION
            'No fue posible verificar la cuenta administrativa Adri Admin.';
    END IF;
END;
$$;

COMMIT;
