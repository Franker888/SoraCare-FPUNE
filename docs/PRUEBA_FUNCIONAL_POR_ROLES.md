# Prueba funcional integral por roles

Estado: iniciada; continuidad de sesión confirmada por el usuario; resto pendiente. No equivale a homologación
fiscal ni a certificación de seguridad. SIFEN y Google Calendar permanecen aplazados.

Usar cuentas de prueba y datos ficticios. No compartir contraseñas, realizar pagos
reales ni enviar documentos a terceros. No alterar el reloj ni desactivar controles
para iniciar atenciones futuras: preparar una cita válida en el horario de prueba.

## Recorrido y criterios de aprobación

| Bloque | Comprobación | Estado |
| --- | --- | --- |
| Acceso | Inicio de sesión normal; Google Identity por separado; cierre de sesión efectivo | Pendiente |
| Sesión paciente | Ir del panel a inicio, especialidades y médicos; volver sin perder la sesión | Aprobado según prueba manual del usuario |
| Agenda | Reservar turno disponible, ver la misma cita en paciente y recepción; evitar duplicados | Pendiente |
| Cambios de cita | Reprogramar/cancelar y comprobar disponibilidad en los roles involucrados | Pendiente |
| Recepción | Confirmar pago de prueba y comprobar estado y comprobante; no cobrar realmente | Pendiente |
| Médico | Ver la cita correcta, iniciar atención en horario permitido, guardar y cerrar atención | Pendiente |
| Paciente | Consultar indicaciones y descargar documentos propios; comprobar contenido ficticio esperado | Pendiente |
| Facturación interna | Emitir y consultar documento interno; no confundirlo con factura fiscal | Pendiente |
| Administración | Revisar agenda/reportes y coherencia de estados, cantidades e importes de la prueba | Pendiente |
| Permisos | Otro paciente/médico de prueba no accede a citas, documentos o facturas ajenos | Pendiente |
| Regresión | Repetir los casos afectados por cada corrección y comprobar cierre de sesión | Pendiente |

Para cada ejecución registrar cuenta de prueba (sin contraseña), fecha/hora, paso,
resultado esperado, resultado observado e identificador de la cita ficticia.
No copiar datos clínicos reales ni capturas con secretos. Marcar aprobado solo
con evidencia del recorrido; pruebas unitarias y HTTP anónimo son complementarias.

## Primer paso con el usuario

Ingresar como paciente de prueba y recorrer inicio → especialidades → médicos →
panel. Confirmar que la sesión se mantiene y señalar cualquier solicitud inesperada
de iniciar sesión. El usuario confirmó: «Sí se conserva la sesión».
Evidencia: reporte del usuario; sin captura ni verificación independiente del agente.
Esta confirmación no valida todavía cierre de sesión, expiración ni Google Identity.

Siguiente paso: reservar una cita con cuenta de prueba y motivo ficticio,
comprobarla en Mis citas y registrar médico, fecha, hora y estado. No realizar
pagos reales. La visualización en recepción y la prevención de duplicados siguen pendientes.
