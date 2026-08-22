<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.AgendaMedicoDTO" %>
<%@ page import="py.fpune.soracare.dto.CitaAgendaMedicoDTO" %>
<%@ page import="py.fpune.soracare.mvc.model.Medico" %>
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

    private String claseEstado(String codigo) {
        if ("CONFIRMADA".equals(codigo)) return "estado-confirmada";
        if ("ATENDIDA".equals(codigo)) return "estado-atendida";
        if ("EN_ATENCION".equals(codigo)) return "estado-atencion";
        if ("AUSENTE".equals(codigo)) return "estado-ausente";
        return "estado-pendiente";
    }
%>
<%
    AgendaMedicoDTO agenda = (AgendaMedicoDTO) request.getAttribute("agenda");
    LocalDate fecha = (LocalDate) request.getAttribute("fecha");
    LocalDate hoy = LocalDate.now(DisponibilidadService.ZONA_HORARIA);
    if (fecha == null) fecha = hoy;
    boolean esHoy = fecha.equals(hoy);
    List<CitaAgendaMedicoDTO> citas = agenda == null ? List.of() : agenda.citas();
    Medico medico = agenda == null ? null : agenda.medico();
    String error = (String) request.getAttribute("error");
    DateTimeFormatter hora = DateTimeFormatter.ofPattern("HH:mm");
    DateTimeFormatter fechaLarga = DateTimeFormatter.ofPattern(
            "EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "PY"));
    long confirmadas = citas.stream()
            .filter(cita -> "CONFIRMADA".equals(cita.codigoEstado())).count();
    long atendidas = citas.stream()
            .filter(cita -> "ATENDIDA".equals(cita.codigoEstado())).count();
    long enAtencion = citas.stream()
            .filter(cita -> "EN_ATENCION".equals(cita.codigoEstado())).count();
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>Mi agenda médica | SoraCare-FPUNE</title>
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css"
          rel="stylesheet">
    <style>
        body { background: #f3f7fb; color: #172033; }
        .barra-superior { background: #ffffff; }
        .marca { color: #235ee7; font-weight: 800; }
        .cabecera-medica {
            border: 0;
            border-radius: 1.25rem;
            color: #ffffff;
            background: linear-gradient(135deg, #1747c7, #268fc8);
        }
        .tarjeta { border: 1px solid #dfe7f1; border-radius: 1.1rem; }
        .resumen { border: 0; border-radius: 1rem; }
        .numero-resumen { font-size: 1.8rem; font-weight: 800; }
        .cita { border: 1px solid #dfe7f1; border-radius: 1rem; }
        .hora-cita { color: #1747c7; font-size: 1.05rem; font-weight: 800; }
        .badge-estado { border-radius: 999px; padding: .45rem .7rem; }
        .estado-confirmada { background: #ccfbf1; color: #0f766e; }
        .estado-atendida { background: #dbeafe; color: #1d4ed8; }
        .estado-atencion { background: #ede9fe; color: #6d28d9; }
        .estado-ausente { background: #fee2e2; color: #b91c1c; }
        .estado-pendiente { background: #fef3c7; color: #92400e; }
        .etiqueta-dato { color: #64748b; font-size: .8rem; }
        .fecha-legible { text-transform: capitalize; }
    </style>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815">
</head>
<body class="app-shell app-doctor">
<nav class="navbar barra-superior border-bottom">
    <div class="container py-2">
        <a class="navbar-brand marca" href="${pageContext.request.contextPath}/medico/inicio">
            SoraCare-FPUNE
        </a>
        <div class="d-flex align-items-center gap-2">
            <a class="btn btn-outline-primary btn-sm"
               href="${pageContext.request.contextPath}/medico/inicio">Panel</a>
            <form method="post" action="${pageContext.request.contextPath}/logout" class="m-0">
                <button type="submit" class="btn btn-outline-secondary btn-sm">Cerrar sesión</button>
            </form>
        </div>
    </div>
</nav>

<main class="container py-4 py-md-5">
    <section class="card cabecera-medica shadow-sm mb-4">
        <div class="card-body p-4 p-md-5">
            <div class="row align-items-center g-3">
                <div class="col-lg-8">
                    <p class="mb-2 opacity-75">Portal médico · Etapa 6</p>
                    <h1 class="display-6 fw-bold mb-2">
                        <%= esHoy ? "Agenda de hoy" : "Agenda médica" %>
                    </h1>
                    <p class="mb-0">
                        <% if (medico != null) { %>
                        <%= medico.getTratamiento() == null
                                ? "Dr." : h(medico.getTratamiento().getAbreviatura()) %>
                        <%= h(medico.getNombres() + " " + medico.getApellidos()) %>
                        · Matrícula <%= h(medico.getMatriculaProfesional()) %>
                        <% } else { %>
                        Consultá las citas asignadas a tu perfil profesional.
                        <% } %>
                    </p>
                </div>
                <div class="col-lg-4 text-lg-end">
                    <span class="badge bg-white text-primary fs-6 px-3 py-2 fecha-legible">
                        <%= esHoy ? "Hoy · " : "" %><%= h(fecha.format(fechaLarga)) %>
                    </span>
                </div>
            </div>
        </div>
    </section>

    <% if (error != null && !error.isBlank()) { %>
    <div class="alert alert-danger" role="alert">
        <strong>No pudimos abrir la agenda.</strong>
        <div class="mt-1"><%= h(error) %></div>
    </div>
    <% } %>

    <section class="card tarjeta shadow-sm mb-4">
        <div class="card-body p-4">
            <div class="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-3">
                <div>
                    <h2 class="h5 mb-1">Consultar otra fecha</h2>
                    <p class="text-secondary mb-0">
                        Al ingresar siempre se muestran automáticamente las citas de hoy.
                    </p>
                </div>
                <% if (esHoy) { %>
                <span class="badge text-bg-primary px-3 py-2">Mostrando hoy</span>
                <% } else { %>
                <span class="badge text-bg-secondary px-3 py-2">Consultando otro día</span>
                <% } %>
            </div>
            <form method="get" action="${pageContext.request.contextPath}/medico/agenda"
                  class="row g-3 align-items-end">
                <div class="col-md-4">
                    <label for="fecha" class="form-label fw-semibold">Ir a una fecha</label>
                    <input type="date" id="fecha" name="fecha" class="form-control"
                           value="<%= fecha %>" required>
                </div>
                <div class="col-md-3 d-grid">
                    <button class="btn btn-primary">Ver agenda</button>
                </div>
                <div class="col-md-5 d-flex flex-wrap justify-content-md-end gap-2">
                    <% if (!esHoy) { %>
                    <a class="btn btn-primary"
                       href="${pageContext.request.contextPath}/medico/agenda">Hoy</a>
                    <% } %>
                    <a class="btn btn-outline-secondary"
                       href="${pageContext.request.contextPath}/medico/agenda?fecha=<%= fecha.minusDays(1) %>">Anterior</a>
                    <a class="btn btn-outline-secondary"
                       href="${pageContext.request.contextPath}/medico/agenda?fecha=<%= fecha.plusDays(1) %>">Siguiente</a>
                </div>
            </form>
        </div>
    </section>

    <% if (agenda != null) { %>
    <section class="row g-3 mb-4">
        <div class="col-6 col-lg-3">
            <div class="card resumen shadow-sm h-100"><div class="card-body">
                <div class="etiqueta-dato"><%= esHoy ? "Citas de hoy" : "Citas del día" %></div>
                <div class="numero-resumen"><%= citas.size() %></div>
            </div></div>
        </div>
        <div class="col-6 col-lg-3">
            <div class="card resumen shadow-sm h-100"><div class="card-body">
                <div class="etiqueta-dato">Confirmadas</div>
                <div class="numero-resumen text-success"><%= confirmadas %></div>
            </div></div>
        </div>
        <div class="col-6 col-lg-3">
            <div class="card resumen shadow-sm h-100"><div class="card-body">
                <div class="etiqueta-dato">En atención</div>
                <div class="numero-resumen" style="color:#6d28d9"><%= enAtencion %></div>
            </div></div>
        </div>
        <div class="col-6 col-lg-3">
            <div class="card resumen shadow-sm h-100"><div class="card-body">
                <div class="etiqueta-dato">Atendidas</div>
                <div class="numero-resumen text-primary"><%= atendidas %></div>
            </div></div>
        </div>
    </section>

    <section class="card tarjeta shadow-sm">
        <div class="card-body p-4">
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-4">
                <div>
                    <h2 class="h4 mb-1">Pacientes agendados</h2>
                    <p class="text-secondary mb-0">Sólo se muestran las citas vinculadas con tu perfil médico.</p>
                </div>
                <span class="badge text-bg-primary"><%= citas.size() %> cita<%= citas.size() == 1 ? "" : "s" %></span>
            </div>

            <% if (citas.isEmpty()) { %>
            <div class="text-center border rounded-4 py-5 px-3">
                <h3 class="h5">
                    <%= esHoy
                            ? "Hoy no tenés pacientes agendados"
                            : "No hay pacientes agendados" %>
                </h3>
                <p class="text-secondary mb-0">
                    <%= esHoy
                            ? "Tu agenda de hoy no posee citas activas."
                            : "No existen citas activas para la fecha seleccionada." %>
                </p>
            </div>
            <% } else { %>
            <div class="d-grid gap-3">
                <% for (CitaAgendaMedicoDTO cita : citas) { %>
                <article class="cita p-3 p-md-4">
                    <div class="row g-3 align-items-start">
                        <div class="col-md-2">
                            <div class="hora-cita">
                                <%= cita.fechaHoraInicio().atZoneSameInstant(
                                        DisponibilidadService.ZONA_HORARIA).format(hora) %>
                                –
                                <%= cita.fechaHoraFin().atZoneSameInstant(
                                        DisponibilidadService.ZONA_HORARIA).format(hora) %>
                            </div>
                            <span class="badge badge-estado <%= claseEstado(cita.codigoEstado()) %> mt-2">
                                <%= h(cita.nombreEstado()) %>
                            </span>
                        </div>
                        <div class="col-md-4">
                            <div class="etiqueta-dato">Paciente</div>
                            <h3 class="h5 mb-1"><%= h(cita.pacienteNombreCompleto()) %></h3>
                            <div class="text-secondary small">
                                CI <%= h(cita.cedulaPaciente()) %>
                                · HC <%= h(cita.numeroHistoriaClinica()) %>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="etiqueta-dato">Consulta</div>
                            <div class="fw-semibold"><%= h(cita.especialidad()) %></div>
                            <div class="text-secondary small"><%= h(cita.consultorio()) %></div>
                        </div>
                        <div class="col-md-3">
                            <div class="etiqueta-dato">Motivo</div>
                            <div><%= h(cita.motivoConsulta()) %></div>
                            <% if (cita.observaciones() != null
                                    && !cita.observaciones().isBlank()) { %>
                            <div class="text-secondary small mt-1">
                                <%= h(cita.observaciones()) %>
                            </div>
                            <% } %>
                        </div>
                        <div class="col-12 d-flex justify-content-end border-top pt-3">
                            <a class="btn <%= "EN_ATENCION".equals(cita.codigoEstado())
                                    ? "btn-primary" : "btn-outline-primary" %>"
                               href="${pageContext.request.contextPath}/medico/atencion?idCita=<%= cita.idCita() %>">
                                <%= "EN_ATENCION".equals(cita.codigoEstado())
                                        ? "Continuar atención"
                                        : "Abrir panel del paciente" %>
                            </a>
                        </div>
                    </div>
                </article>
                <% } %>
            </div>
            <% } %>
        </div>
    </section>
    <% } %>
</main>
<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
</body>
</html>
