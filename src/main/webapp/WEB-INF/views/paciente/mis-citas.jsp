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
<%@ page import="java.time.OffsetDateTime" %>
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
    DateTimeFormatter formatoDiaSemana = DateTimeFormatter.ofPattern(
            "EEE", new Locale("es", "PY"));
    DateTimeFormatter formatoDiaMes = DateTimeFormatter.ofPattern(
            "MMM yyyy", new Locale("es", "PY"));
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>Mis citas | SoraCare-FPUNE</title>
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap-icons/bootstrap-icons.min.css" rel="stylesheet">
    <style>
        body { background: #f4f8f6; }
        .barra { background: #0f766e; }
        .tarjeta { border: 0; border-radius: 1rem; }
        .slot { border-left: .3rem solid #15956e; }
    </style>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260820b">
</head>
<body class="app-shell app-patient app-booking">
<nav class="navbar barra shadow-sm">
    <div class="container py-2">
        <a class="navbar-brand fw-bold text-white" href="${pageContext.request.contextPath}/paciente/inicio">SoraCare-FPUNE</a>
        <div class="d-flex gap-2">
            <a class="btn btn-outline-light btn-sm" href="${pageContext.request.contextPath}/paciente/inicio">Panel</a>
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
    <section class="card tarjeta shadow-sm mb-4 booking-card booking-search-card">
        <div class="card-body p-4 p-lg-5">
            <div class="booking-section-heading">
                <span class="booking-step">1</span>
                <div>
                    <h2>Buscar un turno</h2>
                    <p>Primero elegí el área médica, el profesional y una fecha con disponibilidad.</p>
                </div>
            </div>
            <form method="get" action="${pageContext.request.contextPath}/mis-citas" class="row g-4 booking-search-form">
                <input type="hidden" name="nueva" value="1">
                <div class="col-lg-6">
                    <div class="booking-field">
                        <label for="idEspecialidad" class="form-label">
                            <i class="bi bi-heart-pulse" aria-hidden="true"></i>
                            Especialidad
                        </label>
                        <div class="booking-control">
                            <i class="bi bi-search" aria-hidden="true"></i>
                            <select id="idEspecialidad" name="idEspecialidad" class="form-select">
                                <option value="">Todas las especialidades</option>
                                <% for (Especialidad especialidad : especialidadesLista) { %>
                                <option value="<%= especialidad.getIdEspecialidad() %>" <%= especialidad.getIdEspecialidad().equals(idEspecialidad) ? "selected" : "" %>><%= h(especialidad.getNombre()) %></option>
                                <% } %>
                            </select>
                        </div>
                        <small>Seleccioná el tipo de atención que necesitás.</small>
                    </div>
                </div>
                <div class="col-lg-6">
                    <div class="booking-field">
                        <label for="idMedico" class="form-label">
                            <i class="bi bi-person-badge" aria-hidden="true"></i>
                            Profesional
                        </label>
                        <div class="booking-control">
                            <i class="bi bi-person" aria-hidden="true"></i>
                            <select id="idMedico" name="idMedico" class="form-select">
                                <option value="">Todos los médicos</option>
                                <% for (Medico medico : medicosLista) { %>
                                <option value="<%= medico.getIdMedico() %>"
                                        data-especialidades="<%= idsEspecialidades(especialidadesPorMedico.getOrDefault(medico.getIdMedico(), List.of())) %>"
                                        <%= medico.getIdMedico().equals(idMedico) ? "selected" : "" %>><%= h(medico.getNombres() + " " + medico.getApellidos()) %></option>
                                <% } %>
                            </select>
                        </div>
                        <small>Podés ver todos o elegir un profesional específico.</small>
                        <div id="sinMedicosEspecialidad" class="form-text text-danger d-none">
                            No hay médicos activos para esta especialidad.
                        </div>
                    </div>
                </div>
                <div class="col-12">
                    <fieldset class="booking-calendar" <%= fechasDisponibles.isEmpty() ? "disabled" : "" %>>
                        <legend>
                            <span><i class="bi bi-calendar2-week" aria-hidden="true"></i> Fecha disponible</span>
                            <small>Deslizá para explorar los próximos días habilitados.</small>
                        </legend>
                        <% if (fechasDisponibles.isEmpty()) { %>
                        <div class="booking-calendar-empty">
                            <i class="bi bi-calendar2-x" aria-hidden="true"></i>
                            <span>Sin fechas disponibles</span>
                        </div>
                        <% } else { %>
                        <div class="booking-date-strip" role="radiogroup" aria-label="Fechas disponibles">
                            <% for (int indiceFecha = 0; indiceFecha < fechasDisponibles.size(); indiceFecha++) {
                                LocalDate fechaDisponible = fechasDisponibles.get(indiceFecha);
                                boolean fechaMarcada = fechaDisponible.equals(fecha)
                                        || (fecha == null && indiceFecha == 0);
                            %>
                            <div class="booking-date-option">
                                <input type="radio"
                                       id="fecha-<%= fechaDisponible %>"
                                       name="fecha"
                                       value="<%= fechaDisponible %>"
                                       <%= fechaMarcada ? "checked" : "" %>
                                       required>
                                <label for="fecha-<%= fechaDisponible %>"
                                       title="<%= h(formatoFechaDisponible.format(fechaDisponible)) %>">
                                    <span class="booking-date-weekday"><%= h(formatoDiaSemana.format(fechaDisponible)) %></span>
                                    <strong><%= fechaDisponible.getDayOfMonth() %></strong>
                                    <span class="booking-date-month"><%= h(formatoDiaMes.format(fechaDisponible)) %></span>
                                    <i class="bi bi-check-circle-fill" aria-hidden="true"></i>
                                </label>
                            </div>
                            <% } %>
                        </div>
                        <% } %>
                    </fieldset>
                </div>
                <div class="col-12 d-flex justify-content-end">
                    <button class="btn btn-primary booking-search-button" name="busqueda" value="turnos"
                            <%= fechasDisponibles.isEmpty() ? "disabled" : "" %>>
                        <i class="bi bi-clock" aria-hidden="true"></i>
                        Ver horarios disponibles
                        <i class="bi bi-arrow-right" aria-hidden="true"></i>
                    </button>
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
    <section class="card tarjeta shadow-sm mb-4 booking-card booking-request-card">
        <div class="card-body p-4 p-lg-5">
            <div class="booking-section-heading booking-section-heading-with-status">
                <span class="booking-step">2</span>
                <div>
                    <h2>Elegí el horario y contanos el motivo</h2>
                    <p>Seleccioná uno de los turnos libres para continuar con la reserva.</p>
                </div>
                <span class="booking-availability-badge">
                    <i class="bi bi-check2-circle" aria-hidden="true"></i>
                    <%= slots.size() %> turnos disponibles
                </span>
            </div>
            <form method="post" action="${pageContext.request.contextPath}/mis-citas" class="row g-4">
                <input type="hidden" name="fecha" value="<%= fecha %>">
                <input type="hidden" name="nueva" value="1">
                <input type="hidden" name="busqueda" value="turnos">
                <input type="hidden" name="idMedico" value="<%= idMedico == null ? "" : idMedico %>">
                <input type="hidden" name="idEspecialidad" value="<%= idEspecialidad == null ? "" : idEspecialidad %>">
                <div class="col-12">
                    <div class="booking-field booking-field-highlight">
                        <label for="slot" class="form-label">
                            <i class="bi bi-clock-history" aria-hidden="true"></i>
                            Horario disponible
                        </label>
                        <div class="booking-control">
                            <i class="bi bi-calendar2-check" aria-hidden="true"></i>
                            <select class="form-select" id="slot" name="slot" required>
                                <option value="">Seleccionar horario disponible</option>
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
                        <small>El horario elegido quedará reservado cuando completes esta solicitud.</small>
                    </div>
                </div>
                <div class="col-md-7">
                    <div class="booking-field">
                        <label for="motivoConsulta" class="form-label">
                            <i class="bi bi-chat-left-text" aria-hidden="true"></i>
                            Motivo de consulta
                        </label>
                        <div class="booking-control">
                            <i class="bi bi-pencil" aria-hidden="true"></i>
                            <input id="motivoConsulta" name="motivoConsulta" class="form-control" maxlength="500" required placeholder="Ej.: control cardiológico">
                        </div>
                        <small>Describí brevemente la razón principal de tu consulta.</small>
                    </div>
                </div>
                <div class="col-md-5">
                    <div class="booking-field">
                        <label for="observaciones" class="form-label">
                            <i class="bi bi-journal-text" aria-hidden="true"></i>
                            Observaciones <span>(opcional)</span>
                        </label>
                        <div class="booking-control">
                            <i class="bi bi-plus-circle" aria-hidden="true"></i>
                            <input id="observaciones" name="observaciones" class="form-control" maxlength="1000" placeholder="Agregá información útil para el médico">
                        </div>
                        <small>Podés mencionar síntomas, estudios previos u otra referencia.</small>
                    </div>
                </div>
                <div class="col-12">
                    <section class="booking-payment-panel">
                        <div class="booking-payment-heading">
                            <span class="booking-step">3</span>
                            <div>
                                <h3>Pago y datos del comprobante</h3>
                                <p>Elegí cómo abonar y revisá los datos que aparecerán en tu comprobante.</p>
                            </div>
                            <span class="booking-secure-pill">
                                <i class="bi bi-shield-check" aria-hidden="true"></i>
                                Entorno de prueba
                            </span>
                        </div>
                        <div class="row g-4">
                            <div class="col-md-4">
                                <div class="booking-field">
                                    <label for="metodoPago" class="form-label"><i class="bi bi-credit-card" aria-hidden="true"></i> Método de pago</label>
                                    <div class="booking-control">
                                        <i class="bi bi-wallet2" aria-hidden="true"></i>
                                        <select id="metodoPago" name="metodoPago" class="form-select" required>
                                            <option value="">Seleccionar método</option>
                                            <% for (MetodoPago metodo : metodosPago) { %>
                                            <option value="<%= h(metodo.getCodigo()) %>"><%= h(metodo.getNombre()) %></option>
                                            <% } %>
                                        </select>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-3">
                                <div class="booking-field">
                                    <label for="tipoDocumentoFactura" class="form-label"><i class="bi bi-file-earmark-person" aria-hidden="true"></i> Tipo de documento</label>
                                    <div class="booking-control">
                                        <i class="bi bi-card-text" aria-hidden="true"></i>
                                        <select id="tipoDocumentoFactura" name="tipoDocumentoFactura" class="form-select" required>
                                            <option value="CI">Cédula</option><option value="RUC">RUC</option><option value="PASAPORTE">Pasaporte</option>
                                        </select>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-5">
                                <div class="booking-field">
                                    <label for="numeroDocumentoFactura" class="form-label"><i class="bi bi-person-vcard" aria-hidden="true"></i> Número de documento</label>
                                    <div class="booking-control">
                                        <i class="bi bi-123" aria-hidden="true"></i>
                                        <input id="numeroDocumentoFactura" name="numeroDocumentoFactura" class="form-control" maxlength="30" required value="<%= paciente == null ? "" : h(paciente.getCedula()) %>">
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="booking-field">
                                    <label for="nombreFactura" class="form-label"><i class="bi bi-person-check" aria-hidden="true"></i> Nombre o razón social</label>
                                    <div class="booking-control">
                                        <i class="bi bi-person" aria-hidden="true"></i>
                                        <input id="nombreFactura" name="nombreFactura" class="form-control" maxlength="160" required value="<%= paciente == null ? "" : h(paciente.getNombres() + " " + paciente.getApellidos()) %>">
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="booking-field">
                                    <label for="correoFactura" class="form-label"><i class="bi bi-envelope-check" aria-hidden="true"></i> Correo para el comprobante</label>
                                    <div class="booking-control">
                                        <i class="bi bi-envelope" aria-hidden="true"></i>
                                        <input id="correoFactura" name="correoFactura" type="email" class="form-control" maxlength="255" required value="<%= paciente == null ? "" : h(paciente.getCorreo()) %>">
                                    </div>
                                </div>
                            </div>
                            <div class="col-12">
                                <div class="booking-field">
                                    <label for="direccionFactura" class="form-label"><i class="bi bi-geo-alt" aria-hidden="true"></i> Dirección para facturación</label>
                                    <div class="booking-control">
                                        <i class="bi bi-house-door" aria-hidden="true"></i>
                                        <input id="direccionFactura" name="direccionFactura" class="form-control" maxlength="255" required value="<%= paciente == null ? "" : h(paciente.getDireccion()) %>">
                                    </div>
                                </div>
                            </div>
                        </div>
                    </section>
                </div>
                <div class="col-12">
                    <div class="booking-submit-bar">
                        <div>
                            <i class="bi bi-info-circle" aria-hidden="true"></i>
                            <span>En efectivo, el turno queda pendiente de pago. Los medios digitales continúan a la simulación.</span>
                        </div>
                        <button class="btn btn-primary">
                            <i class="bi bi-calendar2-check" aria-hidden="true"></i>
                            Reservar y continuar al pago
                            <i class="bi bi-arrow-right" aria-hidden="true"></i>
                        </button>
                    </div>
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
                boolean reprogramable = List.of("SOLICITADA", "PENDIENTE_PAGO", "CONFIRMADA")
                        .contains(cita.getCodigoEstado())
                        && cita.getFechaHoraInicio().isAfter(
                                OffsetDateTime.now(DisponibilidadService.ZONA_HORARIA));
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
                                    <% if (pago.idFacturaInterna() != null) { %>
                                    <a class="btn btn-sm btn-outline-primary" target="_blank" href="${pageContext.request.contextPath}/facturas/interna?id=<%= pago.idFacturaInterna() %>">Ver factura interna PDF</a>
                                    <% } %>
                                </div>
                                <% } %>
                            </div>
                            <div class="col-lg-5">
                                <% if (reprogramable) { %>
                                <div class="d-flex justify-content-lg-end mb-2">
                                    <a class="btn btn-outline-primary btn-sm"
                                       href="${pageContext.request.contextPath}/mis-citas/reprogramar?idCita=<%= cita.getIdCita() %>">
                                        Reprogramar cita
                                    </a>
                                </div>
                                <% } %>
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
<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/app/js/filtro-medicos-especialidad.js"></script>
</body>
</html>
