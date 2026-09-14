<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.DisponibilidadDiaDTO" %>
<%@ page import="py.fpune.soracare.dto.SlotDisponibleDTO" %>
<%@ page import="py.fpune.soracare.mvc.model.Consultorio" %>
<%@ page import="py.fpune.soracare.mvc.model.Especialidad" %>
<%@ page import="py.fpune.soracare.mvc.model.Medico" %>
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
    boolean modoMedico = Boolean.TRUE.equals(request.getAttribute("modoMedico"));
    LocalDate desde = request.getAttribute("desde") instanceof LocalDate
            ? (LocalDate) request.getAttribute("desde")
            : LocalDate.now(DisponibilidadService.ZONA_HORARIA);
    int diasConsultados = request.getAttribute("diasConsultados") instanceof Integer
            ? (Integer) request.getAttribute("diasConsultados") : 45;
    int totalTurnos = request.getAttribute("totalTurnos") instanceof Integer
            ? (Integer) request.getAttribute("totalTurnos") : 0;
    Long idMedico = (Long) request.getAttribute("idMedico");
    Integer idEspecialidad = (Integer) request.getAttribute("idEspecialidad");
    Medico medicoSeleccionado = (Medico) request.getAttribute("medicoSeleccionado");
    List<Medico> medicos = request.getAttribute("medicos") instanceof List<?>
            ? (List<Medico>) request.getAttribute("medicos") : List.of();
    List<Especialidad> especialidades = request.getAttribute("especialidades") instanceof List<?>
            ? (List<Especialidad>) request.getAttribute("especialidades") : List.of();
    List<DisponibilidadDiaDTO> agenda = request.getAttribute("agendaDisponible") instanceof List<?>
            ? (List<DisponibilidadDiaDTO>) request.getAttribute("agendaDisponible") : List.of();
    Map<Integer, Especialidad> especialidadesPorId = request.getAttribute("especialidadesPorId") instanceof Map<?, ?>
            ? (Map<Integer, Especialidad>) request.getAttribute("especialidadesPorId") : Map.of();
    Map<Long, Consultorio> consultoriosPorId = request.getAttribute("consultoriosPorId") instanceof Map<?, ?>
            ? (Map<Long, Consultorio>) request.getAttribute("consultoriosPorId") : Map.of();
    String error = (String) request.getAttribute("error");
    DateTimeFormatter fechaLarga = DateTimeFormatter.ofPattern(
            "EEEE d 'de' MMMM", new Locale("es", "PY"));
    DateTimeFormatter fechaCorta = DateTimeFormatter.ofPattern(
            "dd/MM/yyyy", new Locale("es", "PY"));
    String rutaActual = modoMedico
            ? "/medico/disponibilidad"
            : "/recepcion/disponibilidad";
    String rutaInicio = modoMedico ? "/medico/inicio" : "/recepcion/inicio";
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title><%= modoMedico ? "Mis horarios libres" : "Disponibilidad médica" %> | SoraCare-FPUNE</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260820d">
</head>
<body class="app-shell availability-workspace <%= modoMedico ? "app-doctor-home" : "app-reception" %>">
<nav class="navbar <%= modoMedico ? "doctor-navbar" : "reception-navbar" %>">
    <div class="container">
        <a class="navbar-brand" href="${pageContext.request.contextPath}<%= rutaInicio %>">SoraCare-FPUNE</a>
        <div class="d-flex align-items-center gap-2 gap-md-3">
            <a class="btn btn-outline-primary btn-sm" href="${pageContext.request.contextPath}<%= rutaInicio %>">
                <i class="bi bi-arrow-left"></i> Volver al panel
            </a>
            <form method="post" action="${pageContext.request.contextPath}/logout" class="m-0"><input type="hidden" name="csrfToken" value="${requestScope.csrfToken}">
                <button type="submit" class="btn btn-outline-secondary btn-sm">Cerrar sesi&oacute;n</button>
            </form>
        </div>
    </div>
</nav>

<main class="container py-4 py-lg-5">
    <section class="availability-hero mb-4">
        <div>
            <span class="availability-eyebrow">
                <i class="bi <%= modoMedico ? "bi-person-check" : "bi-headset" %>"></i>
                <%= modoMedico ? "Agenda profesional" : "Consulta de recepción" %>
            </span>
            <h1><%= modoMedico ? "Mis horarios libres" : "Disponibilidad de profesionales" %></h1>
            <p>
                <%= modoMedico
                        ? "Revisá los próximos días y horarios que todavía no fueron reservados por pacientes."
                        : "Respondé rápidamente qué días y horarios tiene libre cada médico antes de registrar una cita." %>
            </p>
        </div>
        <span class="availability-hero-icon"><i class="bi bi-calendar2-range"></i></span>
    </section>

    <% if (error != null && !error.isBlank()) { %>
    <div class="alert alert-danger" role="alert">
        <i class="bi bi-exclamation-circle me-2"></i><%= h(error) %>
    </div>
    <% } %>

    <section class="card availability-filter-card mb-4">
        <div class="card-body p-4">
            <div class="availability-section-heading">
                <div>
                    <h2><i class="bi bi-funnel"></i> Definir consulta</h2>
                    <p>Se revisan los pr&oacute;ximos <%= diasConsultados %> d&iacute;as desde la fecha elegida.</p>
                </div>
                <% if (medicoSeleccionado != null) { %>
                <span class="availability-doctor-pill">
                    <i class="bi bi-person-badge"></i><%= h(nombreMedico(medicoSeleccionado)) %>
                </span>
                <% } %>
            </div>
            <form method="get" action="${pageContext.request.contextPath}<%= rutaActual %>"
                  class="row g-3 align-items-end">
                <% if (!modoMedico) { %>
                <div class="col-lg-5">
                    <label class="form-label" for="idMedico">M&eacute;dico</label>
                    <div class="availability-input">
                        <i class="bi bi-person"></i>
                        <select class="form-select" id="idMedico" name="idMedico" required>
                            <option value="">Seleccionar profesional</option>
                            <% for (Medico medico : medicos) { %>
                            <option value="<%= medico.getIdMedico() %>"
                                    <%= medico.getIdMedico().equals(idMedico) ? "selected" : "" %>>
                                <%= h(nombreMedico(medico)) %> · <%= h(medico.getMatriculaProfesional()) %>
                            </option>
                            <% } %>
                        </select>
                    </div>
                </div>
                <div class="col-lg-3">
                    <label class="form-label" for="idEspecialidad">Especialidad <span>(opcional)</span></label>
                    <div class="availability-input">
                        <i class="bi bi-heart-pulse"></i>
                        <select class="form-select" id="idEspecialidad" name="idEspecialidad">
                            <option value="">Todas</option>
                            <% for (Especialidad especialidad : especialidades) { %>
                            <option value="<%= especialidad.getIdEspecialidad() %>"
                                    <%= especialidad.getIdEspecialidad().equals(idEspecialidad) ? "selected" : "" %>>
                                <%= h(especialidad.getNombre()) %>
                            </option>
                            <% } %>
                        </select>
                    </div>
                </div>
                <% } %>
                <div class="<%= modoMedico ? "col-lg-6" : "col-lg-2" %>">
                    <label class="form-label" for="desde">Desde</label>
                    <div class="availability-input">
                        <i class="bi bi-calendar3"></i>
                        <input class="form-control" type="date" id="desde" name="desde"
                               min="<%= LocalDate.now(DisponibilidadService.ZONA_HORARIA) %>"
                               value="<%= desde %>" required>
                    </div>
                </div>
                <div class="<%= modoMedico ? "col-lg-3" : "col-lg-2" %> d-grid">
                    <button class="btn btn-primary availability-search-button">
                        <i class="bi bi-search"></i> Consultar
                    </button>
                </div>
            </form>
        </div>
    </section>

    <% if (idMedico == null && !modoMedico) { %>
    <section class="availability-empty-state">
        <span><i class="bi bi-person-lines-fill"></i></span>
        <h2>Seleccion&aacute un médico</h2>
        <p>Elegí el profesional para consultar sus próximos días y horarios libres.</p>
    </section>
    <% } else { %>
    <section class="row g-3 mb-4" aria-label="Resumen de disponibilidad">
        <div class="col-6 col-lg-4">
            <article class="availability-stat">
                <i class="bi bi-calendar2-check"></i>
                <strong><%= agenda.size() %></strong>
                <span>D&iacute;as con disponibilidad</span>
            </article>
        </div>
        <div class="col-6 col-lg-4">
            <article class="availability-stat">
                <i class="bi bi-clock-history"></i>
                <strong><%= totalTurnos %></strong>
                <span>Horarios libres</span>
            </article>
        </div>
        <div class="col-12 col-lg-4">
            <article class="availability-stat">
                <i class="bi bi-calendar-range"></i>
                <strong><%= diasConsultados %></strong>
                <span>D&iacute;as revisados</span>
            </article>
        </div>
    </section>

    <% if (agenda.isEmpty()) { %>
    <section class="availability-empty-state">
        <span><i class="bi bi-calendar2-x"></i></span>
        <h2>No encontramos horarios libres</h2>
        <p>Probá otra fecha inicial<%= modoMedico ? "." : " o quitá el filtro de especialidad." %></p>
    </section>
    <% } else { %>
    <section class="availability-results">
        <div class="availability-results-heading">
            <div>
                <h2>Pr&oacute;ximos espacios disponibles</h2>
                <p>Todos los horarios mostrados est&aacute;n libres al momento de la consulta.</p>
            </div>
            <span><i class="bi bi-arrow-repeat"></i> Disponibilidad actual</span>
        </div>
        <div class="availability-days-grid">
            <% for (DisponibilidadDiaDTO dia : agenda) { %>
            <article class="availability-day-card">
                <header>
                    <div class="availability-date-block">
                        <span><%= h(dia.fecha().format(DateTimeFormatter.ofPattern("EEE", new Locale("es", "PY")))) %></span>
                        <strong><%= dia.fecha().getDayOfMonth() %></strong>
                    </div>
                    <div>
                        <h3><%= h(dia.fecha().format(fechaLarga)) %></h3>
                        <p><%= dia.cantidadTurnos() %> horario<%= dia.cantidadTurnos() == 1 ? "" : "s" %> libre<%= dia.cantidadTurnos() == 1 ? "" : "s" %></p>
                    </div>
                </header>
                <div class="availability-slots">
                    <% for (SlotDisponibleDTO turno : dia.turnos()) {
                        Especialidad especialidad = especialidadesPorId.get(turno.idEspecialidad());
                        Consultorio consultorio = consultoriosPorId.get(turno.idConsultorio());
                    %>
                    <div class="availability-slot">
                        <span class="availability-slot-time">
                            <i class="bi bi-clock"></i><%= turno.horaInicio() %>–<%= turno.horaFin() %>
                        </span>
                        <span><%= especialidad == null ? "Especialidad" : h(especialidad.getNombre()) %></span>
                        <small><i class="bi bi-geo-alt"></i><%= consultorio == null ? "Consultorio" : h(consultorio.getNombre()) %></small>
                    </div>
                    <% } %>
                </div>
                <footer>
                    <span><i class="bi bi-shield-check"></i> Sin cita asignada</span>
                    <a class="btn btn-sm btn-outline-primary"
                       href="${pageContext.request.contextPath}<%= modoMedico
                               ? "/medico/agenda?fecha=" + dia.fecha()
                               : "/agenda/citas?fecha=" + dia.fecha() + "&idMedico=" + idMedico %>">
                        <%= modoMedico ? "Ver agenda del día" : "Agendar en esta fecha" %>
                        <i class="bi bi-arrow-right"></i>
                    </a>
                </footer>
            </article>
            <% } %>
        </div>
        <p class="availability-updated-note">
            <i class="bi bi-info-circle"></i>
            Un horario puede dejar de estar disponible si otro usuario confirma una cita antes de completar la reserva.
        </p>
    </section>
    <% } %>
    <% } %>
</main>
<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
</body>
</html>
