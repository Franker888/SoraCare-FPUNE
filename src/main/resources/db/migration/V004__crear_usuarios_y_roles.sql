BEGIN;

-- =========================================================
-- TABLA: roles
-- Define los tipos de usuarios del sistema.
-- =========================================================

CREATE TABLE soracare.roles (
                                id_rol SMALLINT
                                    GENERATED ALWAYS AS IDENTITY,

                                nombre VARCHAR(30) NOT NULL,
                                descripcion VARCHAR(255),

                                activo BOOLEAN
                                                   NOT NULL DEFAULT TRUE,

                                creado_en TIMESTAMP WITH TIME ZONE
                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT pk_roles
                                    PRIMARY KEY (id_rol),

                                CONSTRAINT uq_roles_nombre
                                    UNIQUE (nombre),

                                CONSTRAINT ck_roles_nombre
                                    CHECK (BTRIM(nombre) <> '')
);

COMMENT ON TABLE soracare.roles IS
    'Roles que determinan las funciones y permisos de los usuarios';


-- =========================================================
-- TABLA: usuarios
-- Almacena las cuentas utilizadas para iniciar sesión.
-- Una persona puede tener como máximo una cuenta.
-- =========================================================

CREATE TABLE soracare.usuarios (
                                   id_usuario BIGINT
                                       GENERATED ALWAYS AS IDENTITY,

                                   id_persona BIGINT NOT NULL,

                                   nombre_usuario VARCHAR(50) NOT NULL,

    -- Aquí se guardará el hash BCrypt, nunca la contraseña.
                                   contrasena_hash VARCHAR(255) NOT NULL,

                                   estado VARCHAR(20)
                                       NOT NULL DEFAULT 'ACTIVO',

                                   intentos_fallidos SMALLINT
                                       NOT NULL DEFAULT 0,

                                   bloqueado_hasta TIMESTAMP WITH TIME ZONE,

                                   debe_cambiar_contrasena BOOLEAN
                                       NOT NULL DEFAULT TRUE,

                                   ultimo_acceso TIMESTAMP WITH TIME ZONE,

                                   creado_en TIMESTAMP WITH TIME ZONE
                                       NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   actualizado_en TIMESTAMP WITH TIME ZONE
                                       NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT pk_usuarios
                                       PRIMARY KEY (id_usuario),

                                   CONSTRAINT uq_usuarios_persona
                                       UNIQUE (id_persona),

                                   CONSTRAINT fk_usuarios_personas
                                       FOREIGN KEY (id_persona)
                                           REFERENCES soracare.personas (id_persona)
                                           ON UPDATE CASCADE
                                           ON DELETE RESTRICT,

                                   CONSTRAINT ck_usuarios_nombre
                                       CHECK (BTRIM(nombre_usuario) <> ''),

                                   CONSTRAINT ck_usuarios_estado
                                       CHECK (
                                           estado IN (
                                                      'ACTIVO',
                                                      'BLOQUEADO',
                                                      'INACTIVO'
                                               )
                                           ),

                                   CONSTRAINT ck_usuarios_intentos
                                       CHECK (intentos_fallidos >= 0)
);

CREATE UNIQUE INDEX uq_usuarios_nombre_normalizado
    ON soracare.usuarios (LOWER(nombre_usuario));

COMMENT ON TABLE soracare.usuarios IS
    'Cuentas de acceso al sistema SoraCare-FPUNE';

COMMENT ON COLUMN soracare.usuarios.contrasena_hash IS
    'Hash BCrypt de la contraseña; nunca almacena la contraseña original';


-- =========================================================
-- TABLA: usuarios_roles
-- Permite que un usuario tenga uno o varios roles.
-- =========================================================

CREATE TABLE soracare.usuarios_roles (
                                         id_usuario BIGINT NOT NULL,
                                         id_rol SMALLINT NOT NULL,

                                         asignado_en TIMESTAMP WITH TIME ZONE
                                                           NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                         asignado_por BIGINT,

                                         CONSTRAINT pk_usuarios_roles
                                             PRIMARY KEY (id_usuario, id_rol),

                                         CONSTRAINT fk_usuarios_roles_usuario
                                             FOREIGN KEY (id_usuario)
                                                 REFERENCES soracare.usuarios (id_usuario)
                                                 ON UPDATE CASCADE
                                                 ON DELETE CASCADE,

                                         CONSTRAINT fk_usuarios_roles_rol
                                             FOREIGN KEY (id_rol)
                                                 REFERENCES soracare.roles (id_rol)
                                                 ON UPDATE CASCADE
                                                 ON DELETE RESTRICT,

                                         CONSTRAINT fk_usuarios_roles_asignador
                                             FOREIGN KEY (asignado_por)
                                                 REFERENCES soracare.usuarios (id_usuario)
                                                 ON UPDATE CASCADE
                                                 ON DELETE SET NULL
);

COMMENT ON TABLE soracare.usuarios_roles IS
    'Roles asignados a cada usuario del sistema';


-- =========================================================
-- ROLES INICIALES
-- =========================================================

INSERT INTO soracare.roles (nombre, descripcion)
VALUES
    (
        'ADMINISTRADOR',
        'Gestiona usuarios, configuración, reportes y auditoría'
    ),
    (
        'RECEPCIONISTA',
        'Registra pacientes, agenda citas y realiza tareas administrativas'
    ),
    (
        'MEDICO',
        'Atiende pacientes y administra información clínica autorizada'
    ),
    (
        'PACIENTE',
        'Consulta su información personal, citas, pagos y documentos'
    );


-- =========================================================
-- PROPIETARIO DE LAS TABLAS
-- =========================================================

ALTER TABLE soracare.roles
    OWNER TO soracare_app;

ALTER TABLE soracare.usuarios
    OWNER TO soracare_app;

ALTER TABLE soracare.usuarios_roles
    OWNER TO soracare_app;

COMMIT;