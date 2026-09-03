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

## Seguridad y errores

Las operaciones de citas requieren el rol `PACIENTE`. `POST` y `PATCH` exigen
además un token CSRF válido. Las rutas no declaradas se bloquean por defecto.
Los errores se entregan en JSON con `estado`, `codigo` y `mensaje`, sin trazas ni
detalles de la base de datos.

Estados principales:

- `400`: contenido o regla de negocio inválida;
- `401`: no existe una sesión autenticada;
- `403`: falta el rol, la ficha de paciente o el token CSRF;
- `404`: recurso REST no declarado;
- `503`: la operación no pudo completarse por un problema de base de datos.
