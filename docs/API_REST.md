# API REST v1 de SoraCare

La API REST convive con los Servlet y las páginas JSP dentro del mismo WAR. Su
ruta base local en Docker es:

```text
http://localhost:8081/SoraCare-FPUNE/api/v1
```

## Recursos disponibles

### Catálogos públicos

- `GET /especialidades`
- `GET /medicos`
- `GET /disponibilidad?fecha=AAAA-MM-DD`

La disponibilidad acepta también `idMedico` e `idEspecialidad` como filtros
opcionales. Cada turno devuelto contiene los identificadores que necesita la
reserva.

### Sesión del usuario

- `GET /sesion`

Exige la misma sesión iniciada en SoraCare. La respuesta informa los roles y el
token CSRF. El navegador debe conservar la cookie de sesión y enviar ese token
en la cabecera `X-CSRF-Token` para cualquier escritura.

### Citas del paciente

- `GET /citas`: lista únicamente las citas del paciente autenticado.
- `POST /citas`: crea una cita y su pago pendiente en una sola transacción.
- `PATCH /citas/{idCita}/cancelacion`: cancela una cita propia y resuelve su
  pago según las reglas existentes.
- `PATCH /citas/{idCita}/reprogramacion`: mueve una cita propia a otro turno y
  conserva su pago y comprobante.

Ejemplo de creación:

```json
{
  "idHorario": "12",
  "idExcepcion": null,
  "idMedico": "9",
  "idEspecialidad": "3",
  "idConsultorio": "4",
  "fecha": "2026-09-04",
  "horaInicio": "16:00",
  "motivoConsulta": "Control general",
  "observaciones": "Primera consulta",
  "metodoPago": "EFECTIVO",
  "datosFacturacion": {
    "tipoDocumento": "CI",
    "numeroDocumento": "1234567",
    "nombreRazonSocial": "Paciente Prueba",
    "direccion": "Ciudad del Este",
    "correo": "paciente@example.com"
  }
}
```

La respuesta usa HTTP `201 Created` e incluye solo el resumen seguro de la cita
y del pago. El cliente no envía `idPaciente`: el servidor lo obtiene de la
sesión para impedir reservas a nombre de otra persona.

Ejemplo de cancelación:

```json
{
  "motivo": "No podré asistir"
}
```

La cancelación correcta responde `204 No Content`. No es posible cancelar una
cita perteneciente a otro paciente, una ya cancelada o una ya atendida.

Ejemplo de reprogramación:

```json
{
  "idHorario": "14",
  "idExcepcion": null,
  "idMedico": "9",
  "idEspecialidad": "3",
  "idConsultorio": "6",
  "fecha": "2026-09-11",
  "horaInicio": "09:30",
  "motivo": "Cambio de horario laboral"
}
```

La respuesta correcta usa HTTP `200 OK` y devuelve la cita actualizada. La
operación conserva el médico y la especialidad originales, exige un turno
diferente disponible y rechaza citas ajenas, iniciadas o con un estado no
reprogramable.

### Disponibilidad privada del médico

- `GET /medicos/me/disponibilidad?fecha=AAAA-MM-DD`: devuelve los horarios
  base, excepciones propias y citas que impiden bloquear la fecha consultada.
- `POST /medicos/me/disponibilidad/bloqueos`: crea un bloqueo total o parcial.
- `PATCH /medicos/me/disponibilidad/bloqueos/{idExcepcion}`: activa o desactiva
  un bloqueo propio.
- `PATCH /medicos/me/disponibilidad/citas/{idCita}/cancelacion`: cancela una
  cita asignada al médico antes de bloquear el período.

Ejemplo de bloqueo parcial:

```json
{
  "fecha": "2026-09-10",
  "horaInicio": "08:00",
  "horaFin": "10:00",
  "tipo": "BLOQUEO_PARCIAL",
  "motivo": "Capacitación"
}
```

Para un `BLOQUEO_TOTAL`, `horaInicio` y `horaFin` deben ser `null`. El servidor
obtiene el médico desde la sesión y no acepta un `idMedico` enviado por el
cliente. Si el período contiene citas activas, el bloqueo se rechaza hasta que
el médico las cancele o sean reprogramadas.

Ejemplo para desactivar un bloqueo:

```json
{
  "activo": false
}
```

Los horarios recurrentes se consultan desde esta API, pero continúan bajo
administración para evitar cambios masivos accidentales en agendas futuras.

### Información clínica privada del paciente

- `GET /pacientes/me/indicaciones`: lista las indicaciones médicas propias.
- `GET /pacientes/me/indicaciones/{idIndicacion}`: consulta una indicación
  propia con sus medicamentos, dosis y cuidados.
- `GET /pacientes/me/indicaciones/{idIndicacion}/pdf`: abre su PDF.
- `GET /pacientes/me/documentos`: lista los metadatos seguros de los
  documentos clínicos propios.
- `GET /pacientes/me/documentos/{idDocumento}/archivo`: abre el archivo
  clínico después de comprobar su propiedad.

Los dos recursos de archivo aceptan `?descargar=true` para responder como
descarga; sin ese parámetro se abren en línea. Las respuestas JSON no incluyen
el identificador del paciente o la atención, correos de envío, rutas de disco,
hashes ni otros datos internos. Una indicación o documento ajeno nunca se
entrega, aunque el cliente manipule el identificador de la URL.

El PDF corresponde a la indicación farmacológica seleccionada; no representa
el resumen consolidado de toda una atención. Las cargas clínicas actuales se
registran con visibilidad `PACIENTE`. Antes de habilitar documentos reservados
al personal médico o administrativo, debe aplicarse esa distinción tanto al
listado como a la apertura en los servicios compartidos.

## Seguridad y errores

Las operaciones de citas requieren el rol `PACIENTE` y la disponibilidad
privada requiere `MEDICO`. La información clínica bajo `/pacientes/me` exige
el rol `PACIENTE` y solo permite consultas. `POST` y `PATCH` sobre los recursos
que admiten escritura exigen además un token CSRF válido. Las rutas no
declaradas se bloquean por defecto. Todas las respuestas usan `no-store` para
evitar guardar información privada en caché.
Los errores se entregan en JSON con `estado`, `codigo` y `mensaje`, sin trazas ni
detalles de la base de datos.

Estados principales:

- `400`: contenido o regla de negocio inválida;
- `401`: no existe una sesión autenticada;
- `403`: falta el rol, la ficha de paciente o el token CSRF;
- `404`: recurso REST no declarado o indicación/documento no disponible;
- `405`: método no permitido;
- `500`: error interno o archivo que no pudo abrirse;
- `503`: la operación no pudo completarse por un problema de base de datos.
