<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.SlotDisponibleDTO" %>
<%@ page import="py.fpune.soracare.mvc.util.SesionNavegacionPublica" %>
<%@ page import="py.fpune.soracare.mvc.model.Especialidad" %>
<%@ page import="py.fpune.soracare.mvc.model.Medico" %>
<%@ page import="py.fpune.soracare.mvc.model.MedicoEspecialidad" %>
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
    List<Medico> medicos = request.getAttribute("medicos") instanceof List<?>
            ? (List<Medico>) request.getAttribute("medicos") : List.of();
    List<Especialidad> especialidades = request.getAttribute("especialidades") instanceof List<?>
            ? (List<Especialidad>) request.getAttribute("especialidades") : List.of();
    Map<Long, Medico> medicosPorId = request.getAttribute("medicosPorId") instanceof Map<?, ?>
            ? (Map<Long, Medico>) request.getAttribute("medicosPorId") : Map.of();
    Map<Long, List<MedicoEspecialidad>> especialidadesPorMedico =
            request.getAttribute("especialidadesPorMedico") instanceof Map<?, ?>
                    ? (Map<Long, List<MedicoEspecialidad>>) request.getAttribute("especialidadesPorMedico")
                    : Map.of();
    Map<Integer, Especialidad> especialidadesPorId = request.getAttribute("especialidadesPorId") instanceof Map<?, ?>
            ? (Map<Integer, Especialidad>) request.getAttribute("especialidadesPorId") : Map.of();
    LocalDate fecha = (LocalDate) request.getAttribute("fecha");
    Long idMedico = (Long) request.getAttribute("idMedicoSeleccionado");
    Integer idEspecialidad = (Integer) request.getAttribute("idEspecialidadSeleccionada");
    SesionNavegacionPublica navegacion = SesionNavegacionPublica.desde(session);
    String rutaReserva = request.getContextPath() + navegacion.rutaAgenda();
    if (navegacion.paciente()) {
        rutaReserva += "&busqueda=turnos";
        if (fecha != null) rutaReserva += "&fecha=" + fecha;
        if (idMedico != null) rutaReserva += "&idMedico=" + idMedico;
        if (idEspecialidad != null) {
            rutaReserva += "&idEspecialidad=" + idEspecialidad;
        }
    }
    DateTimeFormatter formatoFechaDisponible = DateTimeFormatter.ofPattern(
            "EEEE dd/MM/yyyy", new Locale("es", "PY"));
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Disponibilidad médica | SoraCare-FPUNE</title>
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css" rel="stylesheet">
    <style>
        body { background: #f4f8f6; }
        .hero { background: linear-gradient(135deg, #0b6b4f, #15956e); color: white; }
        .slot { border-left: .3rem solid #15956e; }
    </style>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815">
</head>
<body class="app-public">
<nav class="navbar bg-white border-bottom">
    <div class="container py-2">
        <a class="navbar-brand fw-bold text-success" href="${pageContext.request.contextPath}/">SoraCare-FPUNE</a>
        <span class="d-flex gap-2">
            <a class="btn btn-outline-success btn-sm" href="${pageContext.request.contextPath}/medicos">Ver médicos</a>
            <a class="btn btn-success btn-sm"
               href="<%= h(request.getContextPath() + navegacion.rutaAcceso()) %>">
                <%= h(navegacion.textoAcceso()) %>
            </a>
        </span>
    </div>
</nav>
<header class="hero py-5">
    <div class="container">
        <p class="text-uppercase small mb-2">Agenda médica</p>
        <h1 class="display-6 fw-bold">Disponibilidad de turnos</h1>
        <p class="mb-0">Consultá horarios reales por fecha, profesional y especialidad.</p>
    </div>
</header>
<main class="container py-4">
    <% if (request.getAttribute("error") != null) { %>
    <div class="alert alert-danger"><%= h((String) request.getAttribute("error")) %></div>
    <% } %>
    <% if (request.getAttribute("mensajeDisponibilidad") != null) { %>
    <div class="alert alert-info"><%= h((String) request.getAttribute("mensajeDisponibilidad")) %></div>
    <% } %>
    <section class="card shadow-sm border-0 mb-4">
        <div class="card-body p-4">
            <form method="get" action="${pageContext.request.contextPath}/disponibilidad" class="row g-3 align-items-end">
                <div class="col-md-4">
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
                <div class="col-md-3">
                    <label for="idMedico" class="form-label">Médico</label>
                    <select id="idMedico" name="idMedico" class="form-select">
                        <option value="">Todos los médicos</option>
                        <% for (Medico medico : medicos) { %>
                        <option value="<%= medico.getIdMedico() %>"
                                data-especialidades="<%= idsEspecialidades(especialidadesPorMedico.getOrDefault(medico.getIdMedico(), List.of())) %>"
                                <%= medico.getIdMedico().equals(idMedico) ? "selected" : "" %>>
                            <%= h(medico.getNombres() + " " + medico.getApellidos()) %>
                        </option>
                        <% } %>
                    </select>
                    <div id="sinMedicosEspecialidad" class="form-text text-danger d-none">
                        No hay médicos activos para esta especialidad.
                    </div>
                </div>
                <div class="col-md-3">
                    <label for="idEspecialidad" class="form-label">Especialidad</label>
                    <select id="idEspecialidad" name="idEspecialidad" class="form-select">
                        <option value="">Todas</option>
                        <% for (Especialidad especialidad : especialidades) { %>
                        <option value="<%= especialidad.getIdEspecialidad() %>" <%= especialidad.getIdEspecialidad().equals(idEspecialidad) ? "selected" : "" %>>
                            <%= h(especialidad.getNombre()) %>
                        </option>
                        <% } %>
                    </select>
                </div>
                <div class="col-md-2 d-grid"><button class="btn btn-success" name="busqueda" value="turnos" <%= fechasDisponibles.isEmpty() ? "disabled" : "" %>>Ver turnos</button></div>
            </form>
            <% if (fechasDisponibles.isEmpty()) { %>
            <div class="alert alert-warning mt-3 mb-0">
                No hay fechas con turnos disponibles para la especialidad y el médico seleccionados.
            </div>
            <% } %>
        </div>
    </section>
    <% if (!slots.isEmpty()) { %>
    <div class="d-flex justify-content-between align-items-center mb-3">
        <div><h2 class="h4 mb-1">Turnos del <%= fecha %></h2><p class="text-secondary mb-0">Los turnos ocupados o bloqueados no se muestran.</p></div>
        <span class="badge text-bg-success"><%= slots.size() %> disponibles</span>
    </div>
    <div class="row g-3">
        <% for (SlotDisponibleDTO slot : slots) {
            Medico medico = medicosPorId.get(slot.idMedico());
            Especialidad especialidad = especialidadesPorId.get(slot.idEspecialidad());
        %>
        <div class="col-md-6 col-xl-4">
            <article class="card slot shadow-sm h-100"><div class="card-body">
                <div class="d-flex justify-content-between"><strong class="fs-5"><%= slot.horaInicio() %> – <%= slot.horaFin() %></strong>
                    <% if (slot.disponibilidadExtra()) { %><span class="badge text-bg-info">Horario extra</span><% } %>
                </div>
                <h3 class="h6 mt-3 mb-1"><%= medico == null ? "Médico" : h(medico.getNombres() + " " + medico.getApellidos()) %></h3>
                <p class="text-secondary mb-0"><%= especialidad == null ? "Especialidad" : h(especialidad.getNombre()) %></p>
            </div></article>
        </div>
        <% } %>
    </div>
    <% } %>
    <div class="alert alert-success mt-4 mb-0 d-flex flex-wrap justify-content-between align-items-center gap-3">
        <span><%= h(navegacion.mensajeAgenda()) %></span>
        <span class="d-flex gap-2">
            <a class="btn btn-success btn-sm" href="<%= h(rutaReserva) %>">
                <%= h(navegacion.textoAgenda()) %>
            </a>
            <% if (!navegacion.iniciada()) { %>
            <a class="btn btn-outline-success btn-sm" href="${pageContext.request.contextPath}/registro">Crear cuenta</a>
            <% } %>
        </span>
    </div>
</main>
<script src="${pageContext.request.contextPath}/assets/app/js/filtro-medicos-especialidad.js"></script>
</body>
</html>
