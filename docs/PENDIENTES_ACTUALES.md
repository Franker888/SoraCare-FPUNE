# Pendientes actuales de SoraCare

Actualizado: 11 de septiembre de 2026. Este listado distingue codigo existente,
validacion local y habilitaciones externas; no declara produccion terminada.

## Base implementada

- Disponibilidad medica y bloqueo/reprogramacion de citas.
- Persistencia Jakarta/JPA en el alcance migrado y API REST con permisos.
- Docker, flujo de calidad CI y pruebas PostgreSQL/Flyway aisladas.
- API de consultas clinicas propias y descargas privadas.
- Copia local de base, documentos y fotos, con restauracion aislada comprobada.
- Respaldo diario a las 21:00 configurado en Codex en este equipo.

## Orden de cierre

Decisión del usuario: aplazar SIFEN hasta el final. Conservar lo implementado,
sin nuevas funciones fiscales, simulador ni trámites por ahora. No está homologado.
El siguiente frente recomendado es la prueba funcional integral por roles;
elegir el próximo módulo con el usuario antes de implementar más cambios.

1. **Prueba funcional integral por roles**: paciente, medico, recepcion y
   administracion; registro, Google Identity, continuidad de sesion, agenda,
   atencion, indicaciones, documentos, pagos, facturacion y reportes. Las pruebas
   unitarias y HTTP anonimas no reemplazan este recorrido autenticado.
2. **Integraciones externas**: verificar correo y notificaciones extremo a extremo,
   Twilio, envio servidor Firebase y decidir si corresponde pasarela de pagos.
   Google Calendar permanece oculto y aplazado por decision del usuario.
3. **Produccion**: infraestructura, dominio/HTTPS, permisos, revision de seguridad,
   monitoreo, cifrado y copia externa de respaldos, retencion y simulacro completo
   de recuperacion. El respaldo local no protege ante perdida del equipo.
4. **Visual, después del funcionamiento**: incorporar las referencias que traiga el usuario;
   unificar botones, tipografia, espaciados y estados; revisar todos los paneles
   en escritorio/movil y accesibilidad. No iniciar un rediseno antes de cerrar
   el funcionamiento acordado. Repetir pruebas de regresion tras cambios visuales.
5. **Fiscal — aplazado para el final**: validar formalmente y habilitar la entrega del KuDE al paciente;
   completar conciliación fiscal y análisis/gestión de eventos. El historial
   persistente de consultas está implementado con V037 y V038 agrega observación
   diagnóstica de cancelación informada, sin validar firmas ni conciliar estados. La consulta
   diagnóstica por CDC está implementada, sin alterar el estado fiscal guardado.
   Falta probarla con certificado y CDC oficiales. La descarga de KuDE de prueba para el
   médico está implementada, siempre sin validez fiscal. El motor
   tecnico SIFEN ya existe; la homologacion y autorizacion real requieren los
   datos, certificados y habilitacion del contribuyente. No emitir facturas
   fiscales reales hasta completar esos requisitos.
6. **Evaluacion final solicitada por el usuario**: al terminar el proyecto,
   entregar recomendaciones priorizadas sobre codigo, arquitectura, rendimiento,
   experiencia visual y seguridad. Reservar esa evaluacion para el cierre;
   no adelantar ahora un rediseño o una refactorizacion general.

Las ampliaciones de API REST se priorizaran por un consumidor o caso concreto;
no es necesario exponer cada DAO para considerar cumplida la recomendacion del
profesor. No se marca una integracion como cerrada solo por tener credenciales.
