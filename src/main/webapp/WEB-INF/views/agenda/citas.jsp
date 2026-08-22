<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.SlotDisponibleDTO" %>
<%@ page import="py.fpune.soracare.mvc.model.Cita" %>
<%@ page import="py.fpune.soracare.mvc.model.Consultorio" %>
<%@ page import="py.fpune.soracare.mvc.model.Especialidad" %>
<%@ page import="py.fpune.soracare.mvc.model.Medico" %>
<%@ page import="py.fpune.soracare.mvc.model.Paciente" %>
<%@ page import="py.fpune.soracare.mvc.model.MetodoPago" %>
<%@ page import="py.fpune.soracare.service.DisponibilidadService" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.util.Map" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String nombreMedico(Medico medico) {
        if (medico == null) return "Médico";
        String tratamiento = medico.getTratamiento() == null
                ? "Dr./Dra."
                : medico.getTratamiento().getAbreviatura();
        return tratamiento + " " + medico.getNombres() + " " + medico.getApellidos();
    }
%>
<%
    List<SlotDisponibleDTO> slots = request.getAttribute("slots") instanceof List<?>
            ? (List<SlotDisponibleDTO>) request.getAttribute("slots") : List.of();
    List<Cita> citas = request.getAttribute("citas") instanceof List<?>
            ? (List<Cita>) request.getAttribute("citas") : List.of();
    List<Paciente> pacientes = request.getAttribute("pacientes") instanceof List<?>
            ? (List<Paciente>) request.getAttribute("pacientes") : List.of();
    List<Medico> medicosLista = request.getAttribute("medicos") instanceof List<?>
            ? (List<Medico>) request.getAttribute("medicos") : List.of();
    List<MetodoPago> metodosPago = request.getAttribute("metodosPago") instanceof List<?>
            ? (List<MetodoPago>) request.getAttribute("metodosPago") : List.of();
    Map<Long, Medico> medicos = request.getAttribute("medicosPorId") instanceof Map<?, ?>
            ? (Map<Long, Medico>) request.getAttribute("medicosPorId") : Map.of();
    Map<Integer, Especialidad> especialidades = request.getAttribute("especialidadesPorId") instanceof Map<?, ?>
            ? (Map<Integer, Especialidad>) request.getAttribute("especialidadesPorId") : Map.of();
    Map<Long, Consultorio> consultorios = request.getAttribute("consultoriosPorId") instanceof Map<?, ?>
            ? (Map<Long, Consultorio>) request.getAttribute("consultoriosPorId") : Map.of();
    Map<Long, Paciente> pacientesPorId = request.getAttribute("pacientesPorId") instanceof Map<?, ?>
            ? (Map<Long, Paciente>) request.getAttribute("pacientesPorId") : Map.of();
    LocalDate fecha = request.getAttribute("fecha") instanceof LocalDate
            ? (LocalDate) request.getAttribute("fecha")
            : LocalDate.now(DisponibilidadService.ZONA_HORARIA);
    Long idMedico = (Long) request.getAttribute("idMedico");
    DateTimeFormatter hora = DateTimeFormatter.ofPattern("HH:mm");
    DateTimeFormatter fechaLarga = DateTimeFormatter.ofPattern(
            "EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "PY"));
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title>Agenda de citas | SoraCare-FPUNE</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260820d">
</head>
<body class="app-shell app-reception app-booking reception-agenda">
<%@ include file="../recepcion/_navbar.jspf" %>

<main class="container py-4 py-lg-5">
    <section class="reception-agenda-hero mb-4">
        <div>
            <span class="reception-agenda-eyebrow"><i class="bi bi-calendar2-week"></i> Operación diaria</span>
            <h1>Agenda de citas</h1>
            <p>Consult&aacute; disponibilidad, registr&aacute; pacientes y gestion&aacute; las reservas del centro.</p>
        </div>
        <div class="reception-agenda-hero-actions">
            <a class="btn btn-light" href="${pageContext.request.contextPath}/recepcion/disponibilidad">
                <i class="bi bi-calendar2-range"></i> Disponibilidad médica
            </a>
            <a class="btn btn-outline-light" href="${pageContext.request.contextPath}/pacientes/registro">
                <i class="bi bi-person-plus"></i> Nuevo paciente
            </a>
        </div>
    </section>

    <% if (request.getAttribute("mensajeExito") != null) { %>
    <div class="alert alert-success"><i class="bi bi-check-circle me-2"></i><%= h((String) request.getAttribute("mensajeExito")) %></div>
    <% } %>
    <% if (request.getAttribute("error") != null) { %>
    <div class="alert alert-danger"><i class="bi bi-exclamation-circle me-2"></i><%= h((String) request.getAttribute("error")) %></div>
    <% } %>

    <section class="card reception-agenda-filter mb-4">
        <div class="card-body p-4">
            <div class="reception-agenda-section-title">
                <div>
                    <h2><i class="bi bi-funnel"></i> Consultar agenda</h2>
                    <p>Filtrá por fecha y, si lo necesitás, por un profesional específico.</p>
                </div>
                <span><strong><%= slots.size() %></strong> turnos libres · <strong><%= citas.size() %></strong> citas</span>
            </div>
            <form method="get" action="${pageContext.request.contextPath}/agenda/citas" class="row g-3 align-items-end">
                <div class="col-lg-4">
                    <label class="form-label" for="fecha">Fecha de agenda</label>
                    <div class="availability-input">
                        <i class="bi bi-calendar3"></i>
                        <input class="form-control" type="date" id="fecha" name="fecha"
                               value="<%= fecha %>" required>
                    </div>
                </div>
                <div class="col-lg-5">
                    <label class="form-label" for="idMedico">Médico</label>
                    <div class="availability-input">
                        <i class="bi bi-person-badge"></i>
                        <select class="form-select" id="idMedico" name="idMedico">
                            <option value="">Todos los médicos</option>
                            <% for (Medico medico : medicosLista) { %>
                            <option value="<%= medico.getIdMedico() %>"
                                    <%= medico.getIdMedico().equals(idMedico) ? "selected" : "" %>>
                                <%= h(nombreMedico(medico)) %>
                            </option>
                            <% } %>
                        </select>
                    </div>
                </div>
                <div class="col-lg-3 d-grid">
                    <button class="btn btn-primary reception-agenda-search">
                        <i class="bi bi-search"></i> Consultar agenda
                    </button>
                </div>
            </form>
        </div>
    </section>

    <section class="card booking-card reception-booking-card mb-4">
        <div class="card-body p-4 p-lg-5">
            <div class="booking-section-heading booking-section-heading-with-status">
                <span class="booking-step">1</span>
                <div>
                    <h2>Solicitar nueva cita</h2>
                    <p>Seleccioná el paciente y uno de los horarios libres de la fecha consultada.</p>
                </div>
                <span class="booking-availability-badge"><i class="bi bi-clock"></i><%= slots.size() %> disponibles</span>
            </div>
            <form method="post" action="${pageContext.request.contextPath}/agenda/citas" class="row g-4">
                <input type="hidden" name="accion" value="reservar">
                <input type="hidden" name="fecha" value="<%= fecha %>">
                <input type="hidden" name="idMedico" value="<%= idMedico == null ? "" : idMedico %>">
                <div class="col-md-6">
                    <div class="booking-field">
                        <label class="form-label" for="idPaciente"><i class="bi bi-person-vcard"></i> Paciente</label>
                        <div class="booking-control">
                            <i class="bi bi-person"></i>
                            <select class="form-select" id="idPaciente" name="idPaciente" required>
                                <option value="">Seleccionar paciente</option>
                                <% for (Paciente paciente : pacientes) { %>
                                <option value="<%= paciente.getIdPaciente() %>"
                                        data-documento="<%= h(paciente.getCedula()) %>"
                                        data-nombre="<%= h(paciente.getNombres() + " " + paciente.getApellidos()) %>"
                                        data-correo="<%= h(paciente.getCorreo()) %>"
                                        data-direccion="<%= h(paciente.getDireccion()) %>">
                                    <%= h(paciente.getApellidos() + ", " + paciente.getNombres() + " — " + paciente.getNumeroHistoriaClinica()) %>
                                </option>
                                <% } %>
                            </select>
                        </div>
                        <small>Si todavía no existe, registralo desde el botón “Nuevo paciente”.</small>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="booking-field booking-field-highlight">
                        <label class="form-label" for="slot"><i class="bi bi-clock-history"></i> Turno disponible</label>
                        <div class="booking-control">
                            <i class="bi bi-calendar2-check"></i>
                            <select class="form-select" id="slot" name="slot" required>
                                <option value="">Seleccionar horario</option>
                                <% for (SlotDisponibleDTO slot : slots) {
                                    Medico medico = medicos.get(slot.idMedico());
                                    Especialidad especialidad = especialidades.get(slot.idEspecialidad());
                                    Consultorio consultorio = consultorios.get(slot.idConsultorio());
                                    String valor = (slot.idHorario() == null ? 0 : slot.idHorario()) + "|"
                                            + (slot.idExcepcion() == null ? 0 : slot.idExcepcion()) + "|"
                                            + slot.idMedico() + "|" + slot.idEspecialidad() + "|"
                                            + slot.idConsultorio() + "|" + slot.fecha() + "|" + slot.horaInicio();
                                %>
                                <option value="<%= valor %>"><%= slot.horaInicio() %>–<%= slot.horaFin() %> · <%= h(nombreMedico(medico)) %> · <%= especialidad == null ? "Especialidad" : h(especialidad.getNombre()) %> · <%= consultorio == null ? "Consultorio" : h(consultorio.getNombre()) %></option>
                                <% } %>
                            </select>
                        </div>
                        <small>Los horarios ocupados o bloqueados no aparecen en esta lista.</small>
                    </div>
                </div>
                <div class="col-md-8">
                    <div class="booking-field">
                        <label class="form-label" for="motivoConsulta"><i class="bi bi-chat-left-text"></i> Motivo de consulta</label>
                        <div class="booking-control"><i class="bi bi-pencil"></i><input class="form-control" id="motivoConsulta" name="motivoConsulta" maxlength="500" required placeholder="Ej.: control cardiológico"></div>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="booking-field">
                        <label class="form-label" for="canalReserva"><i class="bi bi-diagram-3"></i> Canal de reserva</label>
                        <div class="booking-control"><i class="bi bi-headset"></i><select class="form-select" id="canalReserva" name="canalReserva"><option value="RECEPCION">Recepción</option><option value="TELEFONO">Teléfono</option><option value="ADMINISTRACION">Administración</option></select></div>
                    </div>
                </div>
                <div class="col-12">
                    <div class="booking-field">
                        <label class="form-label" for="observaciones"><i class="bi bi-journal-text"></i> Observaciones <span>(opcional)</span></label>
                        <textarea class="form-control reception-observations" id="observaciones" name="observaciones" rows="3" maxlength="1000" placeholder="Información adicional para el profesional"></textarea>
                    </div>
                </div>

                <div class="col-12">
                    <section class="booking-payment-panel">
                        <div class="booking-payment-heading">
                            <span class="booking-step">2</span>
                            <div><h3>Pago y comprobante</h3><p>Revisá los datos del paciente antes de confirmar la reserva.</p></div>
                            <span class="booking-secure-pill"><i class="bi bi-shield-check"></i> Entorno académico</span>
                        </div>
                        <div class="row g-4">
                            <div class="col-md-4"><div class="booking-field"><label class="form-label" for="metodoPago"><i class="bi bi-wallet2"></i> Método</label><div class="booking-control"><i class="bi bi-credit-card"></i><select class="form-select" id="metodoPago" name="metodoPago" required><option value="">Seleccionar</option><% for (MetodoPago metodo : metodosPago) { %><option value="<%= h(metodo.getCodigo()) %>" <%= "EFECTIVO".equals(metodo.getCodigo()) ? "selected" : "" %>><%= h(metodo.getNombre()) %></option><% } %></select></div></div></div>
                            <div class="col-md-3"><div class="booking-field"><label class="form-label" for="tipoDocumentoFactura"><i class="bi bi-file-earmark-person"></i> Documento</label><div class="booking-control"><i class="bi bi-card-text"></i><select class="form-select" id="tipoDocumentoFactura" name="tipoDocumentoFactura" required><option value="CI">Cédula</option><option value="RUC">RUC</option><option value="PASAPORTE">Pasaporte</option></select></div></div></div>
                            <div class="col-md-5"><div class="booking-field"><label class="form-label" for="numeroDocumentoFactura"><i class="bi bi-person-vcard"></i> Número</label><div class="booking-control"><i class="bi bi-123"></i><input class="form-control" id="numeroDocumentoFactura" name="numeroDocumentoFactura" maxlength="30" required></div></div></div>
                            <div class="col-md-6"><div class="booking-field"><label class="form-label" for="nombreFactura"><i class="bi bi-person-check"></i> Nombre o razón social</label><div class="booking-control"><i class="bi bi-person"></i><input class="form-control" id="nombreFactura" name="nombreFactura" maxlength="160" required></div></div></div>
                            <div class="col-md-6"><div class="booking-field"><label class="form-label" for="correoFactura"><i class="bi bi-envelope-check"></i> Correo</label><div class="booking-control"><i class="bi bi-envelope"></i><input class="form-control" type="email" id="correoFactura" name="correoFactura" maxlength="255" required></div></div></div>
                            <div class="col-12"><div class="booking-field"><label class="form-label" for="direccionFactura"><i class="bi bi-geo-alt"></i> Dirección</label><div class="booking-control"><i class="bi bi-house-door"></i><input class="form-control" id="direccionFactura" name="direccionFactura" maxlength="255" required></div></div></div>
                        </div>
                    </section>
                </div>
                <div class="col-12">
                    <div class="booking-submit-bar">
                        <div><i class="bi bi-info-circle"></i><span>La cita y su pago pendiente se registrarán en una sola operación.</span></div>
                        <button class="btn btn-primary" <%= slots.isEmpty() || pacientes.isEmpty() ? "disabled" : "" %>><i class="bi bi-calendar2-plus"></i> Reservar con pago <i class="bi bi-arrow-right"></i></button>
                    </div>
                </div>
            </form>
        </div>
    </section>

    <section class="card reception-day-agenda">
        <div class="card-body p-4">
            <div class="reception-day-heading">
                <div>
                    <span>Agenda registrada</span>
                    <h2><%= h(fecha.format(fechaLarga)) %></h2>
                </div>
                <span class="reception-day-count"><%= citas.size() %> cita<%= citas.size() == 1 ? "" : "s" %></span>
            </div>
            <% if (citas.isEmpty()) { %>
            <div class="reception-agenda-empty"><i class="bi bi-calendar2-x"></i><div><strong>No hay citas registradas</strong><span>La agenda está libre para esta fecha.</span></div></div>
            <% } else { %>
            <div class="table-responsive">
                <table class="table align-middle reception-agenda-table">
                    <thead><tr><th>Hora</th><th>Paciente</th><th>Médico</th><th>Especialidad</th><th>Estado</th><th>Acción</th></tr></thead>
                    <tbody>
                    <% for (Cita cita : citas) {
                        Paciente paciente = pacientesPorId.get(cita.getIdPaciente());
                        Medico medico = medicos.get(cita.getIdMedico());
                        Especialidad especialidad = especialidades.get(cita.getIdEspecialidad());
                    %>
                    <tr>
                        <td><strong><%= cita.getFechaHoraInicio().atZoneSameInstant(DisponibilidadService.ZONA_HORARIA).format(hora) %></strong><br><small><%= cita.getFechaHoraFin().atZoneSameInstant(DisponibilidadService.ZONA_HORARIA).format(hora) %></small></td>
                        <td><%= paciente == null ? "Paciente" : h(paciente.getNombres() + " " + paciente.getApellidos()) %></td>
                        <td><%= h(nombreMedico(medico)) %></td>
                        <td><%= especialidad == null ? "Especialidad" : h(especialidad.getNombre()) %></td>
                        <td><span class="badge <%= "CANCELADA".equals(cita.getCodigoEstado()) ? "text-bg-secondary" : "text-bg-warning" %>"><%= h(cita.getNombreEstado()) %></span></td>
                        <td>
                            <% if (!"CANCELADA".equals(cita.getCodigoEstado()) && !"ATENDIDA".equals(cita.getCodigoEstado())) { %>
                            <form method="post" action="${pageContext.request.contextPath}/agenda/citas" class="d-flex gap-2">
                                <input type="hidden" name="accion" value="cancelar"><input type="hidden" name="fecha" value="<%= fecha %>"><input type="hidden" name="idMedico" value="<%= idMedico == null ? "" : idMedico %>"><input type="hidden" name="idCita" value="<%= cita.getIdCita() %>">
                                <input class="form-control form-control-sm" name="motivoCancelacion" required maxlength="500" placeholder="Motivo"><button class="btn btn-sm btn-outline-danger">Cancelar</button>
                            </form>
                            <% } %>
                        </td>
                    </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
            <% } %>
        </div>
    </section>
</main>
<script>
    const pacienteSelect = document.getElementById('idPaciente');
    if (pacienteSelect) {
        pacienteSelect.addEventListener('change', () => {
            const opcion = pacienteSelect.options[pacienteSelect.selectedIndex];
            document.getElementById('numeroDocumentoFactura').value = opcion.dataset.documento || '';
            document.getElementById('nombreFactura').value = opcion.dataset.nombre || '';
            document.getElementById('correoFactura').value = opcion.dataset.correo || '';
            document.getElementById('direccionFactura').value = opcion.dataset.direccion || '';
        });
    }
</script>
<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
</body>
</html>
