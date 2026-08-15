BEGIN;

ALTER TABLE soracare.notificaciones
    ADD COLUMN IF NOT EXISTS id_receta BIGINT,
    ADD COLUMN IF NOT EXISTS fecha_lectura TIMESTAMPTZ;

ALTER TABLE soracare.notificaciones
    DROP CONSTRAINT IF EXISTS fk_notificaciones_receta;

ALTER TABLE soracare.notificaciones
    ADD CONSTRAINT fk_notificaciones_receta
        FOREIGN KEY (id_receta)
        REFERENCES soracare.recetas_medicas (id_receta)
        ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS ix_notificaciones_persona_lectura
    ON soracare.notificaciones (id_persona, fecha_lectura, fecha_creacion DESC);

CREATE INDEX IF NOT EXISTS ix_notificaciones_receta
    ON soracare.notificaciones (id_receta);

INSERT INTO soracare.plantillas_notificacion (
    codigo, id_canal_notificacion, nombre, asunto, contenido, activo
)
SELECT 'RECETA_EMITIDA_SISTEMA', canal.id_canal_notificacion,
       'Nueva indicación en el portal', 'Nueva indicación médica disponible',
       'El médico registró una nueva indicación. Ya puede consultarla y descargarla desde Mis indicaciones médicas.',
       TRUE
  FROM soracare.canales_notificacion canal
 WHERE canal.codigo = 'SISTEMA'
ON CONFLICT (codigo) DO UPDATE SET
    id_canal_notificacion = EXCLUDED.id_canal_notificacion,
    nombre = EXCLUDED.nombre,
    asunto = EXCLUDED.asunto,
    contenido = EXCLUDED.contenido,
    activo = EXCLUDED.activo;

INSERT INTO soracare.plantillas_notificacion (
    codigo, id_canal_notificacion, nombre, asunto, contenido, activo
)
SELECT datos.codigo, canal.id_canal_notificacion,
       datos.nombre, datos.asunto, datos.contenido, TRUE
  FROM soracare.canales_notificacion canal
  CROSS JOIN (VALUES
      ('CITA_SOLICITADA_SISTEMA', 'Cita solicitada en el portal',
       'Cita solicitada', 'La solicitud de cita fue registrada.'),
      ('CITA_REPROGRAMADA_SISTEMA', 'Cita reprogramada en el portal',
       'Cita reprogramada', 'La cita fue reprogramada.'),
      ('CITA_CANCELADA_SISTEMA', 'Cita cancelada en el portal',
       'Cita cancelada', 'La cita fue cancelada.'),
      ('PAGO_CONFIRMADO_SISTEMA', 'Pago confirmado en el portal',
       'Pago confirmado', 'El pago y su comprobante están disponibles.')
  ) AS datos(codigo, nombre, asunto, contenido)
 WHERE canal.codigo = 'SISTEMA'
ON CONFLICT (codigo) DO UPDATE SET
    id_canal_notificacion = EXCLUDED.id_canal_notificacion,
    nombre = EXCLUDED.nombre,
    asunto = EXCLUDED.asunto,
    contenido = EXCLUDED.contenido,
    activo = EXCLUDED.activo;

COMMIT;
