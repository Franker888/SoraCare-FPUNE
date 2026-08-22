<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.SlotDisponibleDTO" %>
<%@ page import="py.fpune.soracare.mvc.model.Cita" %>
<%@ page import="py.fpune.soracare.mvc.model.Consultorio" %>
<%@ page import="py.fpune.soracare.mvc.model.Especialidad" %>
<%@ page import="py.fpune.soracare.mvc.model.Medico" %>
<%@ page import="py.fpune.soracare.mvc.model.Paciente" %>
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
%>
<%
    Cita cita = (Cita) request.getAttribute("cita");
    Paciente paciente = (Paciente) request.getAttribute("paciente");
    Medico medico = (Medico) request.getAttribute("medico");
    Especialidad especialidad = (Especialidad) request.getAttribute("especialidad");
    List<LocalDate> fechasDisponibles = request.getAttribute("fechasDisponibles") instanceof List<?>
            ? (List<LocalDate>) request.getAttribute("fechasDisponibles") : List.of();
    List<SlotDisponibleDTO> slots = request.getAttribute("slots") instanceof List<?>
            ? (List<SlotDisponibleDTO>) request.getAttribute("slots") : List.of();
    Map<Long, Consultorio> consultorios = request.getAttribute("consultoriosPorId") instanceof Map<?, ?>
            ? (Map<Long, Consultorio>) request.getAttribute("consultoriosPorId") : Map.of();
    LocalDate fecha = (LocalDate) request.getAttribute("fecha");
    boolean busquedaRealizada = Boolean.TRUE.equals(request.getAttribute("busquedaRealizada"));
    DateTimeFormatter fechaHora = DateTimeFormatter.ofPattern("EEEE dd 'de' MMMM 'de' yyyy · HH:mm",
            new Locale("es", "PY"));
    DateTimeFormatter fechaDisponible = DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy",
            new Locale("es", "PY"));
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>Reprogramar cita | SoraCare-FPUNE</title>
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css"
          rel="stylesheet">
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815">
    <style>
        body { background: #f3f7fb; color: #14213d; }
        .barra { background: #0f766e; }
        .encabezado-reprogramacion {
            border: 0; border-radius: 1.35rem; color: #fff;
            background: linear-gradient(120deg, #123eaa 0%, #2864e8 56%, #0f8c83 100%);
            overflow: hidden; position: relative;
        }
        .encabezado-reprogramacion::after {
            content: ""; position: absolute; width: 16rem; height: 16rem;
            right: -4rem; bottom: -8rem; border-radius: 50%;
            background: rgba(255,255,255,.1);
        }
        .encabezado-contenido { position: relative; z-index: 1; }
        .tarjeta { border: 1px solid #dfe7f1; border-radius: 1.15rem; }
        .resumen-actual { border-left: .35rem solid #2864e8; }
        .paso {
            width: 2.4rem; height: 2.4rem; display: grid; place-items: center;
            border-radius: .75rem; color: #fff; background: #2864e8; font-weight: 800;
        }
        .aviso-pago { background: #ecfdf5; border: 1px solid #a7f3d0; color: #065f46; }
        .form-select, .form-control { min-height: 3rem; border-radius: .8rem; }
        .boton-primario { background: #2864e8; border-color: #2864e8; }
        .boton-primario:hover { background: #1d4ed8; border-color: #1d4ed8; }
    </style>
</head>
<body class="app-shell app-patient">
<nav class="navbar barra shadow-sm">
    <div class="container py-2">
        <a class="navbar-brand fw-bold text-white"
           href="${pageContext.request.contextPath}/paciente/inicio">SoraCare-FPUNE</a>
        <div class="d-flex gap-2">
            <a class="btn btn-outline-light btn-sm"
               href="${pageContext.request.contextPath}/mis-citas">Mis citas</a>
            <form method="post" action="${pageContext.request.contextPath}/logout" class="m-0">
                <button class="btn btn-light btn-sm">Cerrar sesión</button>
            </form>
        </div>
    </div>
</nav>

<main class="container py-4 py-md-5">
    <section class="card encabezado-reprogramacion shadow-sm mb-4">
        <div class="card-body encabezado-contenido p-4 p-md-5">
            <p class="fw-semibold opacity-75 mb-1">Portal del paciente · Gestión de turnos</p>
            <h1 class="display-6 fw-bold mb-2">Reprogramar cita</h1>
            <p class="lead mb-0">
                <% if (paciente != null) { %>Hola, <%= h(paciente.getNombres()) %>. <% } %>
                Elegí otro horario disponible sin perder tu reserva ni tu pago.
            </p>
        </div>
    </section>

    <% if (request.getAttribute("error") != null) { %>
    <div class="alert alert-danger"><%= h((String) request.getAttribute("error")) %></div>
    <% } %>

    <% if (cita == null) { %>
    <section class="card tarjeta shadow-sm">
        <div class="card-body text-center p-5">
            <h2 class="h4">No se pudo abrir la cita</h2>
            <p class="text-secondary">Volvé a tu historial y seleccioná una cita que siga pendiente.</p>
            <a class="btn btn-outline-primary"
               href="${pageContext.request.contextPath}/mis-citas">Volver a mis citas</a>
        </div>
    </section>
    <% } else { %>
    <section class="card tarjeta resumen-actual shadow-sm mb-4">
        <div class="card-body p-4">
            <div class="row g-3 align-items-center">
                <div class="col-lg-8">
                    <p class="text-primary fw-semibold mb-1">Turno actual</p>
                    <h2 class="h4 mb-2">
                        <%= h(fechaHora.format(cita.getFechaHoraInicio()
                                .atZoneSameInstant(DisponibilidadService.ZONA_HORARIA))) %>
                    </h2>
                    <div><strong><%= medico == null ? "Médico" : h(medico.getNombres()
                            + " " + medico.getApellidos()) %></strong></div>
                    <div class="text-secondary"><%= especialidad == null
                            ? "Especialidad" : h(especialidad.getNombre()) %></div>
                </div>
                <div class="col-lg-4 text-lg-end">
                    <span class="badge text-bg-success px-3 py-2"><%= h(cita.getNombreEstado()) %></span>
                </div>
            </div>
        </div>
    </section>

    <div class="alert aviso-pago d-flex gap-3 align-items-start mb-4">
        <strong>Tu pago se conserva.</strong>
        <span>Si ya pagaste, el pago y su comprobante seguirán vinculados a esta misma cita.
            No se generará un cobro nuevo.</span>
    </div>

    <section class="card tarjeta shadow-sm mb-4">
        <div class="card-body p-4">
            <div class="d-flex gap-3 align-items-center mb-4">
                <span class="paso">1</span>
                <div>
                    <h2 class="h5 mb-1">Elegir una nueva fecha</h2>
                    <p class="text-secondary mb-0">Sólo aparecen fechas disponibles para el mismo profesional.</p>
                </div>
            </div>
            <form method="get" action="${pageContext.request.contextPath}/mis-citas/reprogramar"
                  class="row g-3 align-items-end">
                <input type="hidden" name="idCita" value="<%= cita.getIdCita() %>">
                <div class="col-md-9">
                    <label for="fecha" class="form-label">Nueva fecha disponible</label>
                    <select id="fecha" name="fecha" class="form-select" required
                            <%= fechasDisponibles.isEmpty() ? "disabled" : "" %>>
                        <% if (fechasDisponibles.isEmpty()) { %>
                        <option value="">Sin fechas disponibles</option>
                        <% } else {
                            for (LocalDate disponible : fechasDisponibles) { %>
                        <option value="<%= disponible %>" <%= disponible.equals(fecha)
                                ? "selected" : "" %>><%= h(fechaDisponible.format(disponible)) %></option>
                        <%  }
                           } %>
                    </select>
                </div>
                <div class="col-md-3 d-grid">
                    <button class="btn boton-primario text-white" name="busqueda" value="turnos"
                            <%= fechasDisponibles.isEmpty() ? "disabled" : "" %>>Ver horarios</button>
                </div>
            </form>
            <% if (fechasDisponibles.isEmpty()) { %>
            <div class="alert alert-warning mt-3 mb-0">
                No hay otro turno disponible para este médico dentro de los próximos 120 días.
            </div>
            <% } %>
        </div>
    </section>

    <% if (!slots.isEmpty()) { %>
    <section class="card tarjeta shadow-sm mb-4">
        <div class="card-body p-4">
            <div class="d-flex flex-wrap justify-content-between gap-3 align-items-center mb-4">
                <div class="d-flex gap-3 align-items-center">
                    <span class="paso">2</span>
                    <div>
                        <h2 class="h5 mb-1">Confirmar el nuevo turno</h2>
                        <p class="text-secondary mb-0">Seleccioná el horario y contanos por qué lo cambiás.</p>
                    </div>
                </div>
                <span class="badge text-bg-primary"><%= slots.size() %> horario<%= slots.size() == 1
                        ? "" : "s" %></span>
            </div>
            <form method="post" action="${pageContext.request.contextPath}/mis-citas/reprogramar"
                  class="row g-3">
                <input type="hidden" name="idCita" value="<%= cita.getIdCita() %>">
                <input type="hidden" name="fecha" value="<%= fecha %>">
                <input type="hidden" name="busqueda" value="turnos">
                <div class="col-12">
                    <label for="slot" class="form-label">Nuevo horario</label>
                    <select id="slot" name="slot" class="form-select" required>
                        <option value="">Seleccionar horario disponible</option>
                        <% for (SlotDisponibleDTO slot : slots) {
                            Consultorio consultorio = consultorios.get(slot.idConsultorio());
                            String valor = (slot.idHorario() == null ? 0 : slot.idHorario()) + "|"
                                    + (slot.idExcepcion() == null ? 0 : slot.idExcepcion()) + "|"
                                    + slot.idMedico() + "|" + slot.idEspecialidad() + "|"
                                    + slot.idConsultorio() + "|" + slot.fecha() + "|" + slot.horaInicio();
                        %>
                        <option value="<%= valor %>"><%= slot.horaInicio() %>–<%= slot.horaFin() %>
                            · <%= consultorio == null ? "Consultorio" : h(consultorio.getNombre()) %></option>
                        <% } %>
                    </select>
                </div>
                <div class="col-12">
                    <label for="motivoReprogramacion" class="form-label">Motivo de reprogramación</label>
                    <textarea id="motivoReprogramacion" name="motivoReprogramacion"
                              class="form-control" rows="3" maxlength="500" required
                              placeholder="Ej.: no podré asistir en la fecha original."></textarea>
                    <div class="form-text">Este motivo quedará registrado en el historial de la cita.</div>
                </div>
                <div class="col-12 d-flex flex-wrap gap-2 justify-content-end">
                    <a class="btn btn-outline-secondary"
                       href="${pageContext.request.contextPath}/mis-citas">Volver sin cambios</a>
                    <button class="btn boton-primario text-white">Confirmar reprogramación</button>
                </div>
            </form>
        </div>
    </section>
    <% } else if (busquedaRealizada && !fechasDisponibles.isEmpty()) { %>
    <div class="alert alert-info">
        Ya no quedan horarios libres para esa fecha. Seleccioná otra y volvé a consultar.
    </div>
    <% } %>
    <% } %>
</main>
<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
</body>
</html>
