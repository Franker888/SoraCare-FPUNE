# Facturación interna de SoraCare

Este módulo permite que el médico emita un documento comercial interno para
una consulta cuyo pago ya fue confirmado. El documento se genera en PDF, se
guarda en PostgreSQL y queda disponible tanto para el profesional como para el
paciente desde sus respectivos portales.

## Reglas del flujo

- solamente el médico asociado a la cita puede emitir la factura;
- el pago debe estar en estado `CONFIRMADO`;
- existe como máximo una factura interna por pago;
- los datos del receptor se copian desde `datos_facturacion_pago` para que el
  documento emitido no cambie si luego se modifica el perfil;
- la cabecera y el detalle se guardan dentro de la misma transacción;
- el PDF y su hash SHA-256 se almacenan en la base de datos;
- el paciente recibe un aviso en el portal;
- si Resend está configurado, el PDF también se envía como adjunto;
- administradores y recepcionistas pueden descargar el archivo, pero no
  emitirlo desde el portal médico.

## Seguridad y consistencia

La emisión usa un bloqueo de fila sobre el pago antes de consultar o insertar
la factura. Esto evita documentos duplicados incluso si se envían dos
solicitudes al mismo tiempo. El formulario exige token anti-CSRF y la descarga
verifica que la persona autenticada sea el paciente, el médico responsable o
personal administrativo autorizado.

## Límite tributario

La numeración `FI-AAAA-NNNNNNNN` pertenece únicamente a SoraCare. La migración
`V033` impide marcar estos documentos como tributarios: no poseen timbrado,
CDC, firma digital ni aprobación de e-Kuatia/SIFEN.

La futura integración con e-Kuatia debe implementarse como otro módulo, con
credenciales y certificados del emisor, ambientes de prueba y producción,
estados fiscales, CDC, eventos y representación gráfica tributaria. No se debe
reutilizar la numeración interna como numeración fiscal.
