BEGIN;

ALTER TABLE soracare.comprobantes_pago
    DROP CONSTRAINT IF EXISTS chk_comprobantes_estado_email;

ALTER TABLE soracare.comprobantes_pago
    ADD CONSTRAINT chk_comprobantes_estado_email
        CHECK (estado_envio_email IN (
            'NO_SOLICITADO',
            'SIMULADO_ENVIADO',
            'PENDIENTE',
            'ENVIADO',
            'FALLIDO',
            'NO_CONFIGURADO'
        ));

COMMENT ON COLUMN soracare.comprobantes_pago.estado_envio_email IS
    'Estado del envio del comprobante: pendiente, aceptado por Resend, fallido o sin proveedor configurado';

COMMIT;
