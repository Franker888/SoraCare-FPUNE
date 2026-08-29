<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.CitaAgendaMedicoDTO" %>
<%@ page import="py.fpune.soracare.dto.ResumenDisponibilidadMedicoDTO" %>
<%@ page import="py.fpune.soracare.mvc.model.ExcepcionHorarioMedico" %>
<%@ page import="py.fpune.soracare.mvc.model.TipoExcepcionHorario" %>
<%@ page import="py.fpune.soracare.service.DisponibilidadService" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String nombreTipo(TipoExcepcionHorario tipo) {
        if (tipo == TipoExcepcionHorario.BLOQUEO_TOTAL) return "Día completo";
        if (tipo == TipoExcepcionHorario.BLOQUEO_PARCIAL) return "Intervalo de horas";
        return "Disponibilidad extra";
    }
%>
<%
    ResumenDisponibilidadMedicoDTO resumen =
            (ResumenDisponibilidadMedicoDTO) request.getAttribute("resumenDisponibilidad");
    LocalDate fecha = (LocalDate) request.getAttribute("fechaSeleccionada");
    if (fecha == null) {
        fecha = LocalDate.now(DisponibilidadService.ZONA_HORARIA).plusDays(1);
    }
    List<CitaAgendaMedicoDTO> citas = resumen == null
            ? List.of() : resumen.citasAfectadas();
    List<ExcepcionHorarioMedico> excepciones = resumen == null
            ? List.of() : resumen.excepciones();
    String csrfToken = (String) request.getAttribute("csrfToken");
    String error = (String) request.getAttribute("error");
    String mensajeExito = (String) request.getAttribute("mensajeExito");
    String tipoFormulario = request.getParameter("tipo");
    if (!"BLOQUEO_PARCIAL".equals(tipoFormulario)) {
        tipoFormulario = "BLOQUEO_TOTAL";
    }
    DateTimeFormatter hora = DateTimeFormatter.ofPattern("HH:mm");
    DateTimeFormatter fechaCorta = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    DateTimeFormatter fechaLarga = DateTimeFormatter.ofPattern(
            "EEEE d 'de' MMMM", new Locale("es", "PY"));
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title>Mi disponibilidad | SoraCare-FPUNE</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css">
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/libs/bootstrap-icons/bootstrap-icons.min.css">
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815e">
    <style>
        body { background: #f3f7fb; color: #172033; }
        .availability-hero {
            border: 0;
            border-radius: 1.25rem;
            color: #fff;
            background: linear-gradient(135deg, #1747c7, #187e9f);
        }
        .availability-card {
            border: 1px solid #dfe7f1;
            border-radius: 1.1rem;
        }
        .step-number {
            display: inline-flex;
            width: 2rem;
            height: 2rem;
            align-items: center;
            justify-content: center;
            border-radius: 50%;
            background: #dbeafe;
            color: #1d4ed8;
            font-weight: 800;
        }
        .appointment-row { border: 1px solid #fecaca; border-radius: .9rem; }
        .exception-row { border-bottom: 1px solid #e5eaf1; }
        .exception-row:last-child { border-bottom: 0; }
        .date-readable { text-transform: capitalize; }
    </style>
</head>
<body class="app-shell app-doctor">
<%@ include file="_navbar.jspf" %>

<main class="container py-4 py-lg-5">
    <section class="card availability-hero shadow-sm mb-4">
        <div class="card-body p-4 p-lg-5">
            <div class="row align-items-center g-3">
                <div class="col-lg-8">
                    <p class="mb-2 opacity-75">Portal médico · Gestión personal</p>
                    <h1 class="display-6 fw-bold mb-2">Mi disponibilidad</h1>
                    <p class="mb-0">
                        Bloqueá un día completo o sólo un intervalo para impedir nuevas reservas.
                    </p>
                </div>
                <div class="col-lg-4 text-lg-end">
                    <a class="btn btn-light text-primary fw-semibold"
                       href="${pageContext.request.contextPath}/medico/agenda?fecha=<%= fecha %>">
                        <i class="bi bi-calendar2-week me-1"></i> Ver agenda del día
                    </a>
                </div>
            </div>
        </div>
    </section>

    <% if (mensajeExito != null && !mensajeExito.isBlank()) { %>
    <div class="alert alert-success" role="status">
        <i class="bi bi-check-circle-fill me-2"></i><%= h(mensajeExito) %>
    </div>
    <% } %>
    <% if (error != null && !error.isBlank()) { %>
    <div class="alert alert-danger" role="alert">
        <strong>No se pudo completar la operación.</strong>
        <div class="mt-1"><%= h(error) %></div>
    </div>
    <% } %>

    <div class="row g-4 align-items-start">
        <div class="col-lg-5">
            <section class="card availability-card shadow-sm">
                <div class="card-body p-4">
                    <div class="d-flex align-items-center gap-3 mb-3">
                        <span class="step-number">1</span>
                        <div>
                            <h2 class="h5 mb-1">Elegí cuándo no podrás atender</h2>
                            <p class="text-secondary small mb-0">
                                El bloqueo se aplicará a todas tus especialidades y consultorios.
                            </p>
                        </div>
                    </div>

                    <form method="post"
                          action="${pageContext.request.contextPath}/medico/mi-disponibilidad"
                          class="row g-3" id="formBloqueo">
                        <input type="hidden" name="csrfToken" value="<%= h(csrfToken) %>">

                        <div class="col-12">
                            <label for="fecha" class="form-label fw-semibold">Fecha</label>
                            <input type="date" id="fecha" name="fecha" class="form-control"
                                   value="<%= fecha %>"
                                   min="<%= LocalDate.now(DisponibilidadService.ZONA_HORARIA) %>"
                                   required>
                        </div>

                        <div class="col-12">
                            <label for="tipo" class="form-label fw-semibold">Tipo de bloqueo</label>
                            <select id="tipo" name="tipo" class="form-select" required>
                                <option value="BLOQUEO_TOTAL"
                                        <%= "BLOQUEO_TOTAL".equals(tipoFormulario) ? "selected" : "" %>>
                                    Día completo
                                </option>
                                <option value="BLOQUEO_PARCIAL"
                                        <%= "BLOQUEO_PARCIAL".equals(tipoFormulario) ? "selected" : "" %>>
                                    Sólo un intervalo de horas
                                </option>
                            </select>
                        </div>

                        <div class="col-sm-6 campos-hora">
                            <label for="horaInicio" class="form-label fw-semibold">Desde</label>
                            <input type="time" id="horaInicio" name="horaInicio"
                                   class="form-control"
                                   value="<%= h(request.getParameter("horaInicio")) %>">
                        </div>
                        <div class="col-sm-6 campos-hora">
                            <label for="horaFin" class="form-label fw-semibold">Hasta</label>
                            <input type="time" id="horaFin" name="horaFin"
                                   class="form-control"
                                   value="<%= h(request.getParameter("horaFin")) %>">
                        </div>

                        <div class="col-12">
                            <label for="motivo" class="form-label fw-semibold">Motivo</label>
                            <textarea id="motivo" name="motivo" class="form-control"
                                      rows="3" maxlength="255" required
                                      placeholder="Ej.: capacitación, licencia o asunto personal"><%= h(request.getParameter("motivo")) %></textarea>
                        </div>

                        <div class="col-12">
                            <div class="alert alert-info small mb-0">
                                Si existen citas futuras en ese período, primero deberás cancelarlas
                                o reprogramarlas. Nunca se eliminarán automáticamente.
                            </div>
                        </div>

                        <div class="col-12 d-grid">
                            <button type="submit" class="btn btn-primary">
                                <i class="bi bi-calendar-x me-1"></i> Guardar bloqueo
                            </button>
                        </div>
                    </form>
                </div>
            </section>
        </div>

        <div class="col-lg-7">
            <section class="card availability-card shadow-sm mb-4">
                <div class="card-body p-4">
                    <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
                        <div class="d-flex align-items-center gap-3">
                            <span class="step-number">2</span>
                            <div>
                                <h2 class="h5 mb-1">Revisá las citas del día</h2>
                                <p class="text-secondary small mb-0 date-readable">
                                    <%= h(fecha.format(fechaLarga)) %>
                                </p>
                            </div>
                        </div>
                        <form method="get"
                              action="${pageContext.request.contextPath}/medico/mi-disponibilidad"
                              class="d-flex gap-2">
                            <input type="date" name="fecha" class="form-control form-control-sm"
                                   value="<%= fecha %>" required>
                            <button class="btn btn-outline-primary btn-sm">Revisar</button>
                        </form>
                    </div>

                    <% if (citas.isEmpty()) { %>
                    <div class="border rounded-4 p-4 text-center">
                        <i class="bi bi-calendar-check fs-2 text-success"></i>
                        <h3 class="h6 mt-2 mb-1">No hay citas futuras que resolver</h3>
                        <p class="text-secondary small mb-0">
                            Podés guardar un bloqueo para esta fecha.
                        </p>
                    </div>
                    <% } else { %>
                    <div class="alert alert-warning small">
                        Hay <strong><%= citas.size() %></strong> cita<%= citas.size() == 1 ? "" : "s" %>
                        que deben cancelarse o reprogramarse antes de bloquear el período correspondiente.
                    </div>
                    <div class="d-grid gap-3">
                        <% for (CitaAgendaMedicoDTO cita : citas) { %>
                        <article class="appointment-row p-3">
                            <div class="d-flex flex-wrap justify-content-between gap-2 mb-2">
                                <div>
                                    <strong><%= h(cita.pacienteNombreCompleto()) %></strong>
                                    <div class="small text-secondary">
                                        <%= cita.fechaHoraInicio().format(hora) %>–<%= cita.fechaHoraFin().format(hora) %>
                                        · <%= h(cita.especialidad()) %>
                                    </div>
                                </div>
                                <span class="badge text-bg-light align-self-start">
                                    <%= h(cita.nombreEstado()) %>
                                </span>
                            </div>
                            <form method="post"
                                  action="${pageContext.request.contextPath}/medico/mi-disponibilidad"
                                  class="row g-2 align-items-end"
                                  onsubmit="return confirm('¿Confirmás la cancelación de esta cita?');">
                                <input type="hidden" name="csrfToken" value="<%= h(csrfToken) %>">
                                <input type="hidden" name="accion" value="cancelarCita">
                                <input type="hidden" name="idCita" value="<%= cita.idCita() %>">
                                <input type="hidden" name="fecha" value="<%= fecha %>">
                                <div class="col-md">
                                    <label class="form-label small fw-semibold"
                                           for="motivo-cita-<%= cita.idCita() %>">
                                        Motivo que recibirá el paciente
                                    </label>
                                    <input id="motivo-cita-<%= cita.idCita() %>"
                                           type="text" name="motivoCancelacion"
                                           class="form-control form-control-sm"
                                           maxlength="500" required>
                                </div>
                                <div class="col-md-auto d-grid">
                                    <button class="btn btn-outline-danger btn-sm">
                                        Cancelar cita
                                    </button>
                                </div>
                            </form>
                        </article>
                        <% } %>
                    </div>
                    <% } %>
                </div>
            </section>
        </div>
    </div>

    <section class="card availability-card shadow-sm mt-4">
        <div class="card-body p-4">
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                <div>
                    <h2 class="h5 mb-1">Mis bloqueos registrados</h2>
                    <p class="text-secondary small mb-0">
                        Podés desactivar un bloqueo propio cuando vuelvas a estar disponible.
                    </p>
                </div>
                <span class="badge text-bg-primary"><%= excepciones.size() %> registro<%= excepciones.size() == 1 ? "" : "s" %></span>
            </div>

            <% if (excepciones.isEmpty()) { %>
            <div class="border rounded-4 p-4 text-center text-secondary">
                Todavía no registraste bloqueos personales.
            </div>
            <% } else { %>
            <div>
                <% for (ExcepcionHorarioMedico excepcion : excepciones) { %>
                <article class="exception-row py-3">
                    <div class="row align-items-center g-3">
                        <div class="col-md-2">
                            <strong><%= excepcion.getFecha().format(fechaCorta) %></strong>
                        </div>
                        <div class="col-md-3">
                            <%= h(nombreTipo(excepcion.getTipo())) %>
                            <% if (excepcion.getHoraInicio() != null) { %>
                            <div class="small text-secondary">
                                <%= excepcion.getHoraInicio().format(hora) %>–<%= excepcion.getHoraFin().format(hora) %>
                            </div>
                            <% } %>
                        </div>
                        <div class="col-md"><%= h(excepcion.getMotivo()) %></div>
                        <div class="col-md-auto">
                            <span class="badge <%= excepcion.isActivo() ? "text-bg-success" : "text-bg-secondary" %>">
                                <%= excepcion.isActivo() ? "Activo" : "Inactivo" %>
                            </span>
                        </div>
                        <div class="col-md-auto">
                            <form method="post"
                                  action="${pageContext.request.contextPath}/medico/mi-disponibilidad">
                                <input type="hidden" name="csrfToken" value="<%= h(csrfToken) %>">
                                <input type="hidden" name="accion" value="cambiarEstado">
                                <input type="hidden" name="idExcepcion"
                                       value="<%= excepcion.getIdExcepcion() %>">
                                <input type="hidden" name="activo"
                                       value="<%= !excepcion.isActivo() %>">
                                <input type="hidden" name="fecha"
                                       value="<%= excepcion.getFecha() %>">
                                <button class="btn btn-sm <%= excepcion.isActivo() ? "btn-outline-danger" : "btn-outline-success" %>">
                                    <%= excepcion.isActivo() ? "Desactivar" : "Reactivar" %>
                                </button>
                            </form>
                        </div>
                    </div>
                </article>
                <% } %>
            </div>
            <% } %>
        </div>
    </section>
</main>

<script>
    (() => {
        const tipo = document.getElementById('tipo');
        const campos = document.querySelectorAll('.campos-hora');
        const inicio = document.getElementById('horaInicio');
        const fin = document.getElementById('horaFin');
        const actualizar = () => {
            const parcial = tipo.value === 'BLOQUEO_PARCIAL';
            campos.forEach(campo => campo.classList.toggle('d-none', !parcial));
            inicio.required = parcial;
            fin.required = parcial;
            inicio.disabled = !parcial;
            fin.disabled = !parcial;
        };
        tipo.addEventListener('change', actualizar);
        actualizar();
    })();
</script>
</body>
</html>
