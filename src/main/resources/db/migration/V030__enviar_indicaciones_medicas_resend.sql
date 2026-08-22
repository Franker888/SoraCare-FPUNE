BEGIN;

ALTER TABLE soracare.atenciones_medicas
    ADD COLUMN IF NOT EXISTS email_indicaciones_destino VARCHAR(255),
    ADD COLUMN IF NOT EXISTS estado_envio_indicaciones_email VARCHAR(20)
        NOT NULL DEFAULT 'NO_SOLICITADO',
    ADD COLUMN IF NOT EXISTS fecha_envio_indicaciones_email TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS proveedor_indicaciones_email VARCHAR(50),
    ADD COLUMN IF NOT EXISTS referencia_indicaciones_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS detalle_envio_indicaciones_email VARCHAR(500);

ALTER TABLE soracare.atenciones_medicas
    DROP CONSTRAINT IF EXISTS chk_atenciones_estado_indicaciones_email;

ALTER TABLE soracare.atenciones_medicas
    ADD CONSTRAINT chk_atenciones_estado_indicaciones_email
        CHECK (estado_envio_indicaciones_email IN (
            'NO_SOLICITADO',
            'PENDIENTE',
            'ENVIADO',
            'FALLIDO',
            'NO_CONFIGURADO'
        ));

INSERT INTO soracare.plantillas_notificacion (
    codigo,
    id_canal_notificacion,
    nombre,
    asunto,
    contenido,
    activo
)
SELECT
    'INDICACIONES_MEDICAS_EMAIL',
    canal.id_canal_notificacion,
    'Indicaciones médicas por correo',
    'Indicaciones médicas de su consulta - SoraCare-FPUNE',
    'Adjuntamos las indicaciones médicas generadas al finalizar la atención.',
    TRUE
FROM soracare.canales_notificacion canal
WHERE canal.codigo = 'EMAIL'
ON CONFLICT (codigo)
DO UPDATE SET
    id_canal_notificacion = EXCLUDED.id_canal_notificacion,
    nombre = EXCLUDED.nombre,
    asunto = EXCLUDED.asunto,
    contenido = EXCLUDED.contenido,
    activo = EXCLUDED.activo;

COMMENT ON COLUMN soracare.atenciones_medicas.estado_envio_indicaciones_email IS
    'Estado del envío por Resend del PDF de indicaciones generado al finalizar la atención';

COMMENT ON COLUMN soracare.atenciones_medicas.email_indicaciones_destino IS
    'Correo del paciente utilizado para enviar las indicaciones de la atención';

COMMIT;
