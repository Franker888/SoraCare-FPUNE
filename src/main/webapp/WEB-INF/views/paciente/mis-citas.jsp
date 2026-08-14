<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.SlotDisponibleDTO" %>
<%@ page import="py.fpune.soracare.dto.PagoResumenDTO" %>
<%@ page import="py.fpune.soracare.mvc.model.Cita" %>
<%@ page import="py.fpune.soracare.mvc.model.Consultorio" %>
<%@ page import="py.fpune.soracare.mvc.model.Especialidad" %>
<%@ page import="py.fpune.soracare.mvc.model.Medico" %>
<%@ page import="py.fpune.soracare.mvc.model.MedicoEspecialidad" %>
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
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String idsEspecialidades(List<MedicoEspecialidad> relaciones) {
        StringBuilder ids = new StringBuilder();
        for (MedicoEspecialidad relacion : relaciones) {
            if (ids.length() > 0) ids.append(',');
            ids.append(relacion.getIdEspecialidad());
        }
        return ids.toString();
    }
%>
<%
    List<SlotDisponibleDTO> slots = request.getAttribute("slots") instanceof List<?>
            ? (List<SlotDisponibleDTO>) request.getAttribute("slots") : List.of();
    List<LocalDate> fechasDisponibles = request.getAttribute("fechasDisponibles") instanceof List<?>
            ? (List<LocalDate>) request.getAttribute("fechasDisponibles") : List.of();
    List<Cita> citas = request.getAttribute("citas") instanceof List<?>
            ? (List<Cita>) request.getAttribute("citas") : List.of();
    List<Medico> medicosLista = request.getAttribute("medicos") instanceof List<?>
            ? (List<Medico>) request.getAttribute("medicos") : List.of();
    List<Especialidad> especialidadesLista = request.getAttribute("especialidades") instanceof List<?>
            ? (List<Especialidad>) request.getAttribute("especialidades") : List.of();
    List<MetodoPago> metodosPago = request.getAttribute("metodosPago") instanceof List<?>
            ? (List<MetodoPago>) request.getAttribute("metodosPago") : List.of();
    Map<Long, PagoResumenDTO> pagosPorCita = request.getAttribute("pagosPorCita") instanceof Map<?, ?>
            ? (Map<Long, PagoResumenDTO>) request.getAttribute("pagosPorCita") : Map.of();
    Map<Long, Medico> medicos = request.getAttribute("medicosPorId") instanceof Map<?, ?>
            ? (Map<Long, Medico>) request.getAttribute("medicosPorId") : Map.of();
    Map<Long, List<MedicoEspecialidad>> especialidadesPorMedico =
            request.getAttribute("especialidadesPorMedico") instanceof Map<?, ?>
                    ? (Map<Long, List<MedicoEspecialidad>>) request.getAttribute("especialidadesPorMedico")
                    : Map.of();
    Map<Integer, Especialidad> especialidades = request.getAttribute("especialidadesPorId") instanceof Map<?, ?>
            ? (Map<Integer, Especialidad>) request.getAttribute("especialidadesPorId") : Map.of();
    Map<Long, Consultorio> consultorios = request.getAttribute("consultoriosPorId") instanceof Map<?, ?>
            ? (Map<Long, Consultorio>) request.getAttribute("consultoriosPorId") : Map.of();
    Paciente paciente = (Paciente) request.getAttribute("paciente");
    LocalDate fecha = (LocalDate) request.getAttribute("fecha");
    Long idMedico = (Long) request.getAttribute("idMedicoSeleccionado");
    Integer idEspecialidad = (Integer) request.getAttribute("idEspecialidadSeleccionada");
    boolean busquedaRealizada = Boolean.TRUE.equals(request.getAttribute("busquedaRealizada"));
    boolean agendandoNuevaCita = Boolean.TRUE.equals(request.getAttribute("agendandoNuevaCita"));
    DateTimeFormatter formatoFechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    DateTimeFormatter formatoFechaDisponible = DateTimeFormatter.ofPattern(
            "EEEE dd/MM/yyyy", new Locale("es", "PY"));
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>Mis citas | SoraCare-FPUNE</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { background: #f4f8f6; }
        .barra { background: #0f766e; }
        .tarjeta { border: 0; border-radius: 1rem; }
        .slot { border-left: .3rem solid #15956e; }
    </style>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css">
</head>
<body class="app-shell app-patient">
<nav class="navbar barra shadow-sm">
    <div class="container py-2">
        <a class="navbar-brand fw-bold text-white" href="${pageContext.request.contextPath}/dashboard">SoraCare-FPUNE</a>
        <div class="d-flex gap-2">
            <a class="btn btn-outline-light btn-sm" href="${pageContext.request.contextPath}/dashboard">Panel</a>
            <form method="post" action="${pageContext.request.contextPath}/logout" class="m-0">
                <button class="btn btn-light btn-sm">Cerrar sesión</button>
            </form>
        </div>
    </div>
</nav>

<main class="container py-4 py-md-5">
    <div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4">
        <div>
            <p class="text-success fw-semibold mb-1">Portal del paciente</p>
            <h1 class="h2 mb-1"><%= agendandoNuevaCita ? "Agendar nueva cita" : "Mis citas" %></h1>
            <p class="text-secondary mb-0">
                <% if (paciente != null) { %>Hola, <%= h(paciente.getNombres()) %>. <% } %>
                <%= agendandoNuevaCita
                        ? "Elegí especialidad, médico y fecha para consultar los turnos disponibles."
                        : "Consultá tus citas pendientes y confirmadas." %>
            </p>
        </div>
        <% if (agendandoNuevaCita) { %>
        <a class="btn btn-outline-success" href="${pageContext.request.contextPath}/mis-citas">Volver a mis citas</a>
        <% } else { %>
        <a class="btn btn-success" href="${pageContext.request.contextPath}/mis-citas?nueva=1">Agendar nueva cita</a>
        <% } %>
    </div>

    <% if (request.getAttribute("mensajeExito") != null) { %>
    <div class="alert alert-success"><%= h((String) request.getAttribute("mensajeExito")) %></div>
    <% } %>
    <% if (request.getAttribute("error") != null) { %>
    <div class="alert alert-danger"><%= h((String) request.getAttribute("error")) %></div>
    <% } %>
    <% if (request.getAttribute("mensajeDisponibilidad") != null) { %>
    <div class="alert alert-info"><%= h((String) request.getAttribute("mensajeDisponibilidad")) %></div>
    <% } %>

    <% if (agendandoNuevaCita) { %>
    <section class="card tarjeta shadow-sm mb-4">
        <div class="card-body p-4">
            <h2 class="h5 mb-3">1. Buscar un turno</h2>
            <form method="get" action="${pageContext.request.contextPath}/mis-citas" class="row g-3 align-items-end">
                <input type="hidden" name="nueva" value="1">
                <div class="col-md-4">
                    <label for="idEspecialidad" class="form-label">Especialidad</label>
                    <select id="idEspecialidad" name="idEspecialidad" class="form-select">
                        <option value="">Todas</option>
                        <% for (Especialidad especialidad : especialidadesLista) { %>
                        <option value="<%= especialidad.getIdEspecialidad() %>" <%= especialidad.getIdEspecialidad().equals(idEspecialidad) ? "selected" : "" %>><%= h(especialidad.getNombre()) %></option>
                        <% } %>
                    </select>
                </div>
                <div class="col-md-3">
                    <label for="idMedico" class="form-label">Médico</label>
                    <select id="idMedico" name="idMedico" class="form-select">
                        <option value="">Todos los médicos</option>
                        <% for (Medico medico : medicosLista) { %>
                        <option value="<%= medico.getIdMedico() %>"
                                data-especialidades="<%= idsEspecialidades(especialidadesPorMedico.getOrDefault(medico.getIdMedico(), List.of())) %>"
                                <%= medico.getIdMedico().equals(idMedico) ? "selected" : "" %>><%= h(medico.getNombres() + " " + medico.getApellidos()) %></option>
                        <% } %>
                    </select>
                    <div id="sinMedicosEspecialidad" class="form-text text-danger d-none">
                        No hay médicos activos para esta especialidad.
                    </div>
                </div>
                <div class="col-md-3">
                    <label for="fecha" class="form-label">Fecha disponible</label>
                    <select id="fecha" name="fecha" class="form-select" required <%= fechasDisponibles.isEmpty() ? "disabled" : "" %>>
                        <% if (fechasDisponibles.isEmpty()) { %>
                        <option value="">Sin fechas disponibles</option>
                        <% } else { %>
                        <% for (LocalDate fechaDisponible : fechasDisponibles) { %>
                        <option value="<%= fechaDisponible %>" <%= fechaDisponible.equals(fecha) ? "selected" : "" %>>
                            <%= h(formatoFechaDisponible.format(fechaDisponible)) %>
                        </option>
                        <% } %>
                        <% } %>
                    </select>
                </div>
                <div class="col-md-2 d-grid">
                    <button class="btn btn-success" name="busqueda" value="turnos"
                            <%= fechasDisponibles.isEmpty() ? "disabled" : "" %>>Ver turnos</button>
                </div>
            </form>
            <% if (fechasDisponibles.isEmpty()) { %>
            <div class="alert alert-warning mt-3 mb-0">
                No hay fechas con turnos disponibles para la especialidad y el médico seleccionados.
            </div>
            <% } %>
        </div>
    </section>

    <% if (!slots.isEmpty()) { %>
    <section class="card tarjeta shadow-sm mb-4">
        <div class="card-body p-4">
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                <h2 class="h5 mb-0">2. Seleccionar y solicitar</h2>
                <span class="badge text-bg-success"><%= slots.size() %> turnos disponibles</span>
            </div>
            <form method="post" action="${pageContext.request.contextPath}/mis-citas" class="row g-3">
                <input type="hidden" name="fecha" value="<%= fecha %>">
                <input type="hidden" name="nueva" value="1">
                <input type="hidden" name="busqueda" value="turnos">
                <input type="hidden" name="idMedico" value="<%= idMedico == null ? "" : idMedico %>">
                <input type="hidden" name="idEspecialidad" value="<%= idEspecialidad == null ? "" : idEspecialidad %>">
                <div class="col-12">
                    <label for="slot" class="form-label">Turno</label>
                    <select class="form-select" id="slot" name="slot" required>
                        <option value="">Seleccionar turno disponible</option>
                        <% for (SlotDisponibleDTO slot : slots) {
                            Medico medico = medicos.get(slot.idMedico());
                            Especialidad especialidad = especialidades.get(slot.idEspecialidad());
                            Consultorio consultorio = consultorios.get(slot.idConsultorio());
                            String valor = (slot.idHorario() == null ? 0 : slot.idHorario()) + "|"
                                    + (slot.idExcepcion() == null ? 0 : slot.idExcepcion()) + "|"
                                    + slot.idMedico() + "|" + slot.idEspecialidad() + "|"
                                    + slot.idConsultorio() + "|" + slot.fecha() + "|" + slot.horaInicio();
                        %>
                        <option value="<%= valor %>"><%= slot.horaInicio() %>–<%= slot.horaFin() %> · <%= medico == null ? "Médico" : h(medico.getNombres() + " " + medico.getApellidos()) %> · <%= especialidad == null ? "Especialidad" : h(especialidad.getNombre()) %> · <%= consultorio == null ? "Consultorio" : h(consultorio.getNombre()) %></option>
                        <% } %>
                    </select>
                </div>
                <div class="col-md-7">
                    <label for="motivoConsulta" class="form-label">Motivo de consulta</label>
                    <input id="motivoConsulta" name="motivoConsulta" class="form-control" maxlength="500" required placeholder="Ej.: control cardiológico">
                </div>
                <div class="col-md-5">
                    <label for="observaciones" class="form-label">Observaciones (opcional)</label>
                    <input id="observaciones" name="observaciones" class="form-control" maxlength="1000">
                </div>
                <div class="col-12"><hr><h3 class="h6 mb-1">3. Pago y datos del comprobante</h3>
                    <p class="small text-secondary mb-2">Los pagos son una simulación académica. No ingreses datos reales de tarjetas.</p></div>
                <div class="col-md-4">
                    <label for="metodoPago" class="form-label">Método de pago</label>
                    <select id="metodoPago" name="metodoPago" class="form-select" required>
                        <option value="">Seleccionar</option>
                        <% for (MetodoPago metodo : metodosPago) { %>
                        <option value="<%= h(metodo.getCodigo()) %>"><%= h(metodo.getNombre()) %></option>
                        <% } %>
                    </select>
                </div>
                <div class="col-md-3">
                    <label for="tipoDocumentoFactura" class="form-label">Tipo de documento</label>
                    <select id="tipoDocumentoFactura" name="tipoDocumentoFactura" class="form-select" required>
                        <option value="CI">Cédula</option><option value="RUC">RUC</option><option value="PASAPORTE">Pasaporte</option>
                    </select>
                </div>
                <div class="col-md-5">
                    <label for="numeroDocumentoFactura" class="form-label">Número de documento</label>
                    <input id="numeroDocumentoFactura" name="numeroDocumentoFactura" class="form-control" maxlength="30" required value="<%= paciente == null ? "" : h(paciente.getCedula()) %>">
                </div>
                <div class="col-md-6">
                    <label for="nombreFactura" class="form-label">Nombre o razón social</label>
                    <input id="nombreFactura" name="nombreFactura" class="form-control" maxlength="160" required value="<%= paciente == null ? "" : h(paciente.getNombres() + " " + paciente.getApellidos()) %>">
                </div>
                <div class="col-md-6">
                    <label for="correoFactura" class="form-label">Correo para el comprobante</label>
                    <input id="correoFactura" name="correoFactura" type="email" class="form-control" maxlength="255" required value="<%= paciente == null ? "" : h(paciente.getCorreo()) %>">
                </div>
                <div class="col-12">
                    <label for="direccionFactura" class="form-label">Dirección para facturación</label>
                    <input id="direccionFactura" name="direccionFactura" class="form-control" maxlength="255" required value="<%= paciente == null ? "" : h(paciente.getDireccion()) %>">
                </div>
                <div class="col-12 d-flex flex-wrap align-items-center gap-3">
                    <button class="btn btn-success">Reservar y continuar al pago</button>
                    <small class="text-secondary">El efectivo conserva el turno pendiente; los medios digitales continúan a la simulación.</small>
                </div>
            </form>
        </div>
    </section>
    <% } %>

    <% if (busquedaRealizada && slots.isEmpty() && !fechasDisponibles.isEmpty()) { %>
    <div class="alert alert-info mb-4">
        Ya no quedan turnos libres para esa búsqueda. Elegí otra fecha y volvé a consultar.
    </div>
    <% } %>
    <% } %>

    <section>
        <div class="mb-3">
            <h2 class="h4 mb-1">Historial de mis citas</h2>
            <p class="text-secondary mb-0">Aquí aparecen tus citas pendientes y confirmadas.</p>
        </div>
        <% if (citas.isEmpty()) { %>
        <div class="alert alert-light border">No tenés citas pendientes o confirmadas en este momento.</div>
        <% } else { %>
        <div class="row g-3">
            <% for (Cita cita : citas) {
                Medico medico = medicos.get(cita.getIdMedico());
                Especialidad especialidad = especialidades.get(cita.getIdEspecialidad());
                Consultorio consultorio = consultorios.get(cita.getIdConsultorio());
                PagoResumenDTO pago = pagosPorCita.get(cita.getIdCita());
                boolean cancelable = !"CANCELADA".equals(cita.getCodigoEstado()) && !"ATENDIDA".equals(cita.getCodigoEstado());
            %>
            <div class="col-12">
                <article class="card tarjeta slot shadow-sm">
                    <div class="card-body p-4">
                        <div class="row g-3 align-items-center">
                            <div class="col-lg-7">
                                <div class="d-flex flex-wrap gap-2 align-items-center mb-2">
                                    <strong><%= cita.getFechaHoraInicio().atZoneSameInstant(DisponibilidadService.ZONA_HORARIA).format(formatoFechaHora) %></strong>
                                    <span class="badge <%= "CANCELADA".equals(cita.getCodigoEstado()) ? "text-bg-secondary" : "text-bg-success" %>"><%= h(cita.getNombreEstado()) %></span>
                                </div>
                                <div><%= medico == null ? "Médico" : h(medico.getNombres() + " " + medico.getApellidos()) %></div>
                                <div class="text-secondary"><%= especialidad == null ? "Especialidad" : h(especialidad.getNombre()) %> · <%= consultorio == null ? "Consultorio" : h(consultorio.getNombre()) %></div>
                                <small class="text-secondary">Motivo: <%= h(cita.getMotivoConsulta()) %></small>
                                <% if (pago != null) { %>
                                <div class="mt-2 d-flex flex-wrap gap-2 align-items-center">
                                    <span class="badge <%= pago.estaConfirmado() ? "text-bg-success" : ("REEMBOLSADO".equals(pago.codigoEstado()) || "ANULADO".equals(pago.codigoEstado()) ? "text-bg-secondary" : "text-bg-warning") %>">
                                        Pago: <%= h(pago.nombreEstado()) %>
                                    </span>
                                    <small class="text-secondary"><%= h(pago.nombreMetodo()) %> · Gs. <%= pago.monto().setScale(0).toPlainString() %></small>
                                    <% if (pago.estaPendiente() && pago.esDigital() && cancelable) { %>
                                    <a class="btn btn-sm btn-success" href="${pageContext.request.contextPath}/pagos/procesar?idPago=<%= pago.idPago() %>">Continuar pago</a>
                                    <% } %>
                                    <% if (pago.idComprobante() != null) { %>
                                    <a class="btn btn-sm btn-outline-success" target="_blank" href="${pageContext.request.contextPath}/comprobantes/pago?id=<%= pago.idComprobante() %>">Ver comprobante PDF</a>
                                    <% } %>
                                </div>
                                <% } %>
                            </div>
                            <div class="col-lg-5">
                                <% if (cancelable) { %>
                                <form method="post" action="${pageContext.request.contextPath}/mis-citas" class="d-flex gap-2">
                                    <input type="hidden" name="accion" value="cancelar">
                                    <input type="hidden" name="fecha" value="<%= fecha == null ? "" : fecha %>">
                                    <input type="hidden" name="idCita" value="<%= cita.getIdCita() %>">
                                    <input name="motivoCancelacion" class="form-control form-control-sm" maxlength="500" required placeholder="Motivo de cancelación">
                                    <button class="btn btn-outline-danger btn-sm">Cancelar</button>
                                </form>
                                <% } else if (cita.getMotivoCancelacion() != null) { %>
                                <small class="text-secondary">Cancelada: <%= h(cita.getMotivoCancelacion()) %></small>
                                <% } %>
                            </div>
                        </div>
                    </div>
                </article>
            </div>
            <% } %>
        </div>
        <% } %>
    </section>
</main>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/app/js/filtro-medicos-especialidad.js"></script>
</body>
</html>
