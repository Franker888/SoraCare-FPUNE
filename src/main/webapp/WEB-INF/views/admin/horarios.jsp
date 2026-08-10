<%@ page contentType="text/html;charset=UTF-8"
         pageEncoding="UTF-8"
         language="java" %>

<%@ page import="py.fpune.soracare.mvc.model.Consultorio" %>
<%@ page import="py.fpune.soracare.mvc.model.Especialidad" %>
<%@ page import="py.fpune.soracare.mvc.model.HorarioMedico" %>
<%@ page import="py.fpune.soracare.mvc.model.Medico" %>
<%@ page import="py.fpune.soracare.mvc.model.MedicoEspecialidad" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>

<%!
    private String escaparHtml(String texto) {
        if (texto == null) {
            return "";
        }

        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String nombreDia(int diaSemana) {
        switch (diaSemana) {
            case 1:
                return "Lunes";
            case 2:
                return "Martes";
            case 3:
                return "Miércoles";
            case 4:
                return "Jueves";
            case 5:
                return "Viernes";
            case 6:
                return "Sábado";
            case 7:
                return "Domingo";
            default:
                return "Día desconocido";
        }
    }
%>

<%
    List<HorarioMedico> horarios =
            request.getAttribute("horarios") instanceof List<?>
                    ? (List<HorarioMedico>) request.getAttribute("horarios")
                    : List.of();

    List<Medico> medicos =
            request.getAttribute("medicos") instanceof List<?>
                    ? (List<Medico>) request.getAttribute("medicos")
                    : List.of();

    List<Consultorio> consultoriosDisponibles =
            request.getAttribute("consultoriosDisponibles") instanceof List<?>
                    ? (List<Consultorio>) request.getAttribute("consultoriosDisponibles")
                    : List.of();

    Map<Long, Medico> medicosPorId =
            request.getAttribute("medicosPorId") instanceof Map<?, ?>
                    ? (Map<Long, Medico>) request.getAttribute("medicosPorId")
                    : Map.of();

    Map<Integer, Especialidad> especialidadesPorId =
            request.getAttribute("especialidadesPorId") instanceof Map<?, ?>
                    ? (Map<Integer, Especialidad>) request.getAttribute("especialidadesPorId")
                    : Map.of();

    Map<Long, List<MedicoEspecialidad>> especialidadesPorMedico =
            request.getAttribute("especialidadesPorMedico") instanceof Map<?, ?>
                    ? (Map<Long, List<MedicoEspecialidad>>) request.getAttribute("especialidadesPorMedico")
                    : Map.of();

    Map<Long, Consultorio> consultoriosPorId =
            request.getAttribute("consultoriosPorId") instanceof Map<?, ?>
                    ? (Map<Long, Consultorio>) request.getAttribute("consultoriosPorId")
                    : Map.of();

    String error =
            (String) request.getAttribute("error");
    String mensajeExito =
            (String) request.getAttribute("mensajeExito");

    String medicoEspecialidadSeleccionada =
            (String) request.getAttribute("medicoEspecialidadSeleccionada");
    String idConsultorioSeleccionado =
            (String) request.getAttribute("idConsultorioSeleccionado");
    String diaSemanaSeleccionado =
            (String) request.getAttribute("diaSemanaSeleccionado");
    String horaInicio =
            (String) request.getAttribute("horaInicio");
    String horaFin =
            (String) request.getAttribute("horaFin");
    String duracionTurnoMinutos =
            (String) request.getAttribute("duracionTurnoMinutos");
    String fechaDesde =
            (String) request.getAttribute("fechaDesde");
    String fechaHasta =
            (String) request.getAttribute("fechaHasta");

    if (duracionTurnoMinutos == null) {
        duracionTurnoMinutos = "30";
    }

    if (fechaDesde == null) {
        fechaDesde = LocalDate.now().toString();
    }
%>

<!DOCTYPE html>
<html lang="es">

<head>
    <meta charset="UTF-8">

    <meta name="viewport"
          content="width=device-width, initial-scale=1">

    <meta name="robots"
          content="noindex, nofollow">

    <title>
        Horarios médicos | SoraCare-FPUNE
    </title>

    <link
            href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
            rel="stylesheet">

    <style>
        body {
            min-height: 100vh;
            background-color: #f1f5f9;
            color: #0f172a;
        }

        .barra-superior {
            background-color: #0f766e;
        }

        .marca-soracare {
            color: #ffffff;
            font-weight: 700;
        }

        .marca-soracare:hover,
        .marca-soracare:focus {
            color: #ffffff;
        }

        .encabezado-panel {
            border: none;
            border-radius: 1rem;
            background:
                    linear-gradient(
                            135deg,
                            #0f766e,
                            #0e7490
                    );
            color: #ffffff;
        }

        .tarjeta-formulario,
        .tarjeta-listado {
            border: none;
            border-radius: 1rem;
        }

        .etiqueta-activo {
            background-color: #ccfbf1;
            color: #115e59;
        }

        .etiqueta-inactivo {
            background-color: #e2e8f0;
            color: #475569;
        }

        .boton-principal {
            background-color: #0f766e;
            border-color: #0f766e;
        }

        .boton-principal:hover,
        .boton-principal:focus {
            background-color: #115e59;
            border-color: #115e59;
        }
    </style>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css">
</head>

<body class="app-shell app-admin">

<nav class="navbar barra-superior shadow-sm">

    <div class="container">

        <a
                class="navbar-brand marca-soracare"
                href="${pageContext.request.contextPath}/dashboard">

            SoraCare-FPUNE

        </a>

        <div class="d-flex gap-2">
            <a class="btn btn-light btn-sm"
               href="${pageContext.request.contextPath}/admin/excepciones-horarios">
                Gestionar excepciones
            </a>
            <a class="btn btn-outline-light btn-sm"
               href="${pageContext.request.contextPath}/admin/inicio">
                Volver a administración
            </a>
        </div>

    </div>

</nav>

<main class="container py-4 py-md-5">

    <section class="card encabezado-panel shadow-sm mb-4">

        <div class="card-body p-4 p-md-5">

            <div class="row align-items-center">

                <div class="col-12 col-lg-8">

                    <p class="mb-2 opacity-75">
                        Agenda médica
                    </p>

                    <h1 class="display-6 fw-bold mb-3">
                        Horarios médicos
                    </h1>

                    <p class="mb-0">

                        Definí los horarios semanales de atención,
                        su vigencia, duración de turnos y consultorio.

                    </p>

                </div>

                <div class="col-12 col-lg-4 mt-4 mt-lg-0 text-lg-end">

                    <span class="badge text-bg-light fs-6">

                        <%= horarios.size() %>
                        horario<%= horarios.size() == 1 ? "" : "s" %>

                    </span>

                </div>

            </div>

        </div>

    </section>

    <% if (mensajeExito != null
            && !mensajeExito.isBlank()) { %>

    <div class="alert alert-success" role="alert">
        <%= escaparHtml(mensajeExito) %>
    </div>

    <% } %>

    <% if (error != null
            && !error.isBlank()) { %>

    <div class="alert alert-danger" role="alert">
        <strong>No fue posible completar la operación.</strong>
        <div class="mt-1">
            <%= escaparHtml(error) %>
        </div>
    </div>

    <% } %>

    <section class="card tarjeta-formulario shadow-sm mb-4">

        <div class="card-body p-4 p-md-5">

            <h2 class="h4 mb-2">
                Nuevo horario recurrente
            </h2>

            <p class="text-secondary mb-4">

                Solo se muestran combinaciones activas de médico
                y especialidad, además de consultorios disponibles.

            </p>

            <form
                    method="post"
                    action="${pageContext.request.contextPath}/admin/horarios"
                    class="row g-3">

                <input type="hidden" name="accion" value="registrar">

                <div class="col-12 col-lg-7">

                    <label for="medicoEspecialidad" class="form-label">
                        Médico y especialidad
                    </label>

                    <select
                            id="medicoEspecialidad"
                            name="medicoEspecialidad"
                            class="form-select"
                            required>

                        <option value="">
                            Seleccionar médico y especialidad
                        </option>

                        <% for (Medico medico : medicos) {
                            List<MedicoEspecialidad> relaciones =
                                    especialidadesPorMedico.getOrDefault(
                                            medico.getIdMedico(),
                                            List.of()
                                    );

                            for (MedicoEspecialidad relacion : relaciones) {
                                Especialidad especialidad =
                                        especialidadesPorId.get(
                                                relacion.getIdEspecialidad()
                                        );

                                if (especialidad == null) {
                                    continue;
                                }

                                String valor =
                                        medico.getIdMedico()
                                                + ":"
                                                + relacion.getIdEspecialidad();
                        %>

                        <option
                                value="<%= escaparHtml(valor) %>"
                                <%= valor.equals(medicoEspecialidadSeleccionada)
                                        ? "selected"
                                        : "" %>>

                            <%= escaparHtml(medico.getNombres()) %>
                            <%= escaparHtml(medico.getApellidos()) %>
                            — <%= escaparHtml(especialidad.getNombre()) %>
                            <%= relacion.isPrincipal()
                                    ? "(Principal)"
                                    : "" %>

                        </option>

                        <% }
                        } %>

                    </select>

                </div>

                <div class="col-12 col-lg-5">

                    <label for="idConsultorio" class="form-label">
                        Consultorio
                    </label>

                    <select
                            id="idConsultorio"
                            name="idConsultorio"
                            class="form-select"
                            required>

                        <option value="">
                            Seleccionar consultorio
                        </option>

                        <% for (Consultorio consultorio : consultoriosDisponibles) {
                            String idConsultorio =
                                    String.valueOf(
                                            consultorio.getIdConsultorio()
                                    );
                        %>

                        <option
                                value="<%= escaparHtml(idConsultorio) %>"
                                <%= idConsultorio.equals(idConsultorioSeleccionado)
                                        ? "selected"
                                        : "" %>>

                            <%= escaparHtml(consultorio.getCodigo()) %>
                            — <%= escaparHtml(consultorio.getNombre()) %>

                        </option>

                        <% } %>

                    </select>

                </div>

                <div class="col-12 col-md-4">

                    <label for="diaSemana" class="form-label">
                        Día de la semana
                    </label>

                    <select
                            id="diaSemana"
                            name="diaSemana"
                            class="form-select"
                            required>

                        <option value="">
                            Seleccionar día
                        </option>

                        <% for (int dia = 1; dia <= 7; dia++) {
                            String valorDia = String.valueOf(dia);
                        %>

                        <option
                                value="<%= valorDia %>"
                                <%= valorDia.equals(diaSemanaSeleccionado)
                                        ? "selected"
                                        : "" %>>

                            <%= nombreDia(dia) %>

                        </option>

                        <% } %>

                    </select>

                </div>

                <div class="col-6 col-md-2">

                    <label for="horaInicio" class="form-label">
                        Desde
                    </label>

                    <input
                            id="horaInicio"
                            name="horaInicio"
                            type="time"
                            class="form-control"
                            value="<%= escaparHtml(horaInicio) %>"
                            required>

                </div>

                <div class="col-6 col-md-2">

                    <label for="horaFin" class="form-label">
                        Hasta
                    </label>

                    <input
                            id="horaFin"
                            name="horaFin"
                            type="time"
                            class="form-control"
                            value="<%= escaparHtml(horaFin) %>"
                            required>

                </div>

                <div class="col-12 col-md-4">

                    <label for="duracionTurnoMinutos" class="form-label">
                        Duración de cada turno
                    </label>

                    <select
                            id="duracionTurnoMinutos"
                            name="duracionTurnoMinutos"
                            class="form-select"
                            required>

                        <% int[] duraciones = {15, 20, 30, 45, 60};
                            for (int duracion : duraciones) {
                                String valorDuracion =
                                        String.valueOf(duracion);
                        %>

                        <option
                                value="<%= valorDuracion %>"
                                <%= valorDuracion.equals(duracionTurnoMinutos)
                                        ? "selected"
                                        : "" %>>

                            <%= duracion %> minutos

                        </option>

                        <% } %>

                    </select>

                </div>

                <div class="col-12 col-md-6">

                    <label for="fechaDesde" class="form-label">
                        Vigente desde
                    </label>

                    <input
                            id="fechaDesde"
                            name="fechaDesde"
                            type="date"
                            class="form-control"
                            value="<%= escaparHtml(fechaDesde) %>"
                            required>

                </div>

                <div class="col-12 col-md-6">

                    <label for="fechaHasta" class="form-label">
                        Vigente hasta
                    </label>

                    <input
                            id="fechaHasta"
                            name="fechaHasta"
                            type="date"
                            class="form-control"
                            value="<%= escaparHtml(fechaHasta) %>">

                    <div class="form-text">
                        Dejá este campo vacío para una vigencia indefinida.
                    </div>

                </div>

                <div class="col-12">

                    <button
                            type="submit"
                            class="btn btn-primary boton-principal">

                        Registrar horario

                    </button>

                </div>

            </form>

        </div>

    </section>

    <section class="card tarjeta-listado shadow-sm">

        <div class="card-body p-4 p-md-5">

            <div class="d-flex flex-column flex-md-row
                        justify-content-between gap-2 mb-4">

                <div>
                    <h2 class="h4 mb-1">
                        Horarios registrados
                    </h2>
                    <p class="text-secondary mb-0">
                        Agenda semanal y vigencias configuradas
                    </p>
                </div>

                <span class="text-secondary">
                    Total: <%= horarios.size() %>
                </span>

            </div>

            <% if (horarios.isEmpty()) { %>

            <div class="alert alert-secondary mb-0" role="status">
                Todavía no existen horarios médicos registrados.
            </div>

            <% } else { %>

            <div class="table-responsive">

                <table class="table align-middle">

                    <thead>
                    <tr>
                        <th>Médico</th>
                        <th>Especialidad</th>
                        <th>Consultorio</th>
                        <th>Día y hora</th>
                        <th>Vigencia</th>
                        <th>Estado</th>
                        <th class="text-end">Acción</th>
                    </tr>
                    </thead>

                    <tbody>

                    <% for (HorarioMedico horario : horarios) {
                        Medico medico =
                                medicosPorId.get(
                                        horario.getIdMedico()
                                );
                        Especialidad especialidad =
                                especialidadesPorId.get(
                                        horario.getIdEspecialidad()
                                );
                        Consultorio consultorio =
                                consultoriosPorId.get(
                                        horario.getIdConsultorio()
                                );
                    %>

                    <tr>
                        <td>
                            <strong>
                                <%= medico == null
                                        ? "Médico no disponible"
                                        : escaparHtml(
                                                medico.getNombres()
                                                        + " "
                                                        + medico.getApellidos()
                                        ) %>
                            </strong>
                        </td>

                        <td>
                            <%= especialidad == null
                                    ? "Especialidad no disponible"
                                    : escaparHtml(especialidad.getNombre()) %>
                        </td>

                        <td>
                            <%= consultorio == null
                                    ? "Consultorio no disponible"
                                    : escaparHtml(
                                            consultorio.getCodigo()
                                                    + " — "
                                                    + consultorio.getNombre()
                                    ) %>
                        </td>

                        <td>
                            <div>
                                <%= nombreDia(horario.getDiaSemana()) %>
                            </div>
                            <small class="text-secondary">
                                <%= horario.getHoraInicio() %>
                                a <%= horario.getHoraFin() %>
                                · <%= horario.getDuracionTurnoMinutos() %> min
                            </small>
                        </td>

                        <td>
                            <div>
                                Desde <%= horario.getFechaDesde() %>
                            </div>
                            <small class="text-secondary">
                                <%= horario.getFechaHasta() == null
                                        ? "Sin fecha final"
                                        : "Hasta " + horario.getFechaHasta() %>
                            </small>
                        </td>

                        <td>
                            <span class="badge <%= horario.isActivo()
                                    ? "etiqueta-activo"
                                    : "etiqueta-inactivo" %>">
                                <%= horario.isActivo()
                                        ? "Activo"
                                        : "Inactivo" %>
                            </span>
                        </td>

                        <td class="text-end">

                            <form
                                    method="post"
                                    action="${pageContext.request.contextPath}/admin/horarios"
                                    class="d-inline">

                                <input
                                        type="hidden"
                                        name="accion"
                                        value="cambiarEstado">

                                <input
                                        type="hidden"
                                        name="idHorario"
                                        value="<%= horario.getIdHorario() %>">

                                <input
                                        type="hidden"
                                        name="activo"
                                        value="<%= !horario.isActivo() %>">

                                <button
                                        type="submit"
                                        class="btn btn-sm <%= horario.isActivo()
                                                ? "btn-outline-secondary"
                                                : "btn-outline-success" %>">

                                    <%= horario.isActivo()
                                            ? "Inactivar"
                                            : "Reactivar" %>

                                </button>

                            </form>

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

<script
        src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js">
</script>

</body>
</html>
