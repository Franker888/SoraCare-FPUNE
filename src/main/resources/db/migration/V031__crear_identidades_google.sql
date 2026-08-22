BEGIN;

CREATE TABLE soracare.identidades_externas (
    id_identidad BIGINT GENERATED ALWAYS AS IDENTITY,
    id_usuario BIGINT NOT NULL,
    proveedor VARCHAR(30) NOT NULL,
    identificador_externo VARCHAR(255) NOT NULL,
    correo_proveedor VARCHAR(150) NOT NULL,
    correo_verificado BOOLEAN NOT NULL DEFAULT FALSE,
    nombre_mostrado VARCHAR(200),
    foto_url VARCHAR(500),
    vinculado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_identidades_externas
        PRIMARY KEY (id_identidad),

    CONSTRAINT fk_identidades_externas_usuario
        FOREIGN KEY (id_usuario)
            REFERENCES soracare.usuarios (id_usuario)
            ON UPDATE CASCADE
            ON DELETE CASCADE,

    CONSTRAINT uq_identidades_externas_proveedor_identificador
        UNIQUE (proveedor, identificador_externo),

    CONSTRAINT uq_identidades_externas_usuario_proveedor
        UNIQUE (id_usuario, proveedor),

    CONSTRAINT ck_identidades_externas_proveedor
        CHECK (proveedor IN ('GOOGLE')),

    CONSTRAINT ck_identidades_externas_identificador
        CHECK (BTRIM(identificador_externo) <> ''),

    CONSTRAINT ck_identidades_externas_correo
        CHECK (BTRIM(correo_proveedor) <> '')
);

CREATE INDEX idx_identidades_externas_usuario
    ON soracare.identidades_externas (id_usuario);

COMMENT ON TABLE soracare.identidades_externas IS
    'Identidades verificadas por proveedores externos y vinculadas a cuentas de SoraCare';

COMMENT ON COLUMN soracare.identidades_externas.identificador_externo IS
    'Identificador estable sub entregado por el proveedor; no se utiliza el correo como clave externa';

ALTER TABLE soracare.identidades_externas
    OWNER TO soracare_app;

COMMIT;
