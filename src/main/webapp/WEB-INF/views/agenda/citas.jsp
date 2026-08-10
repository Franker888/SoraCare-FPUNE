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
<%@ page import="java.util.Map" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    List<SlotDisponibleDTO> slots = request.getAttribute("slots") instanceof List<?>
            ? (List<SlotDisponibleDTO>) request.getAttribute("slots") : List.of();
    List<Cita> citas = request.getAttribute("citas") instanceof List<?>
            ? (List<Cita>) request.getAttribute("citas") : List.of();
    List<Paciente> pacientes = request.getAttribute("pacientes") instanceof List<?>
            ? (List<Paciente>) request.getAttribute("pacientes") : List.of();
    Map<Long, Medico> medicos = (Map<Long, Medico>) request.getAttribute("medicosPorId");
    Map<Integer, Especialidad> especialidades = (Map<Integer, Especialidad>) request.getAttribute("especialidadesPorId");
    Map<Long, Consultorio> consultorios = (Map<Long, Consultorio>) request.getAttribute("consultoriosPorId");
    Map<Long, Paciente> pacientesPorId = (Map<Long, Paciente>) request.getAttribute("pacientesPorId");
    LocalDate fecha = (LocalDate) request.getAttribute("fecha");
    DateTimeFormatter hora = DateTimeFormatter.ofPattern("HH:mm");
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Agenda de citas | SoraCare-FPUNE</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { background: #f4f7f6; }
        .cabecera { background: linear-gradient(135deg, #073f32, #0c7658); color: white; }
        .card { border: 0; }
    </style>
</head>
<body>
<nav class="navbar bg-white border-bottom"><div class="container py-2">
    <a class="navbar-brand fw-bold text-success" href="${pageContext.request.contextPath}/dashboard">SoraCare-FPUNE</a>
    <a class="btn btn-outline-success btn-sm" href="${pageContext.request.contextPath}/dashboard">Volver al panel</a>
</div></nav>
<header class="cabecera py-4"><div class="container">
    <p class="small text-uppercase mb-1">Etapa 4</p><h1 class="h2 mb-1">Agenda médica</h1>
    <p class="mb-0">Reservá turnos disponibles y consultá las citas del día.</p>
</div></header>
<main class="container py-4">
    <% if (request.getAttribute("mensajeExito") != null) { %><div class="alert alert-success"><%= h((String) request.getAttribute("mensajeExito")) %></div><% } %>
    <% if (request.getAttribute("error") != null) { %><div class="alert alert-danger"><%= h((String) request.getAttribute("error")) %></div><% } %>

    <section class="card shadow-sm mb-4"><div class="card-body p-4">
        <form method="get" action="${pageContext.request.contextPath}/agenda/citas" class="row g-3 align-items-end">
            <div class="col-md-5"><label class="form-label" for="fecha">Fecha de agenda</label>
                <input class="form-control" type="date" id="fecha" name="fecha" value="<%= fecha %>" required></div>
            <div class="col-md-3 d-grid"><button class="btn btn-outline-success">Consultar fecha</button></div>
            <div class="col-md-4 text-md-end text-secondary"><%= slots.size() %> turnos libres · <%= citas.size() %> citas registradas</div>
        </form>
    </div></section>

    <section class="card shadow-sm mb-4"><div class="card-body p-4">
        <h2 class="h4">Solicitar nueva cita</h2>
        <p class="text-secondary">La cita queda en estado Solicitada. El pago y la confirmación corresponden a la etapa 5.</p>
        <form method="post" action="${pageContext.request.contextPath}/agenda/citas" class="row g-3">
            <input type="hidden" name="accion" value="reservar"><input type="hidden" name="fecha" value="<%= fecha %>">
            <div class="col-md-6"><label class="form-label" for="idPaciente">Paciente</label>
                <select class="form-select" id="idPaciente" name="idPaciente" required><option value="">Seleccionar paciente</option>
                    <% for (Paciente paciente : pacientes) { %><option value="<%= paciente.getIdPaciente() %>"><%= h(paciente.getApellidos() + ", " + paciente.getNombres() + " — " + paciente.getNumeroHistoriaClinica()) %></option><% } %>
                </select></div>
            <div class="col-md-6"><label class="form-label" for="slot">Turno disponible</label>
                <select class="form-select" id="slot" name="slot" required><option value="">Seleccionar turno</option>
                    <% for (SlotDisponibleDTO slot : slots) {
                        Medico medico = medicos.get(slot.idMedico());
                        Especialidad esp = especialidades.get(slot.idEspecialidad());
                        Consultorio consultorio = consultorios.get(slot.idConsultorio());
                        String valor = (slot.idHorario() == null ? 0 : slot.idHorario()) + "|"
                                + (slot.idExcepcion() == null ? 0 : slot.idExcepcion()) + "|"
                                + slot.idMedico() + "|" + slot.idEspecialidad() + "|"
                                + slot.idConsultorio() + "|" + slot.fecha() + "|" + slot.horaInicio();
                    %><option value="<%= valor %>"><%= slot.horaInicio() %> — <%= medico == null ? "Médico" : h(medico.getNombres() + " " + medico.getApellidos()) %> · <%= esp == null ? "Especialidad" : h(esp.getNombre()) %> · <%= consultorio == null ? "Consultorio" : h(consultorio.getNombre()) %></option><% } %>
                </select></div>
            <div class="col-md-8"><label class="form-label" for="motivoConsulta">Motivo de consulta</label>
                <input class="form-control" id="motivoConsulta" name="motivoConsulta" maxlength="500" required></div>
            <div class="col-md-4"><label class="form-label" for="canalReserva">Canal de reserva</label>
                <select class="form-select" id="canalReserva" name="canalReserva"><option value="RECEPCION">Recepción</option><option value="TELEFONO">Teléfono</option><option value="ADMINISTRACION">Administración</option></select></div>
            <div class="col-12"><label class="form-label" for="observaciones">Observaciones</label>
                <textarea class="form-control" id="observaciones" name="observaciones" rows="2" maxlength="1000"></textarea></div>
            <div class="col-12"><button class="btn btn-success" <%= slots.isEmpty() || pacientes.isEmpty() ? "disabled" : "" %>>Solicitar cita</button></div>
        </form>
    </div></section>

    <section class="card shadow-sm"><div class="card-body p-4">
        <div class="d-flex justify-content-between"><div><h2 class="h4 mb-1">Citas del <%= fecha %></h2><p class="text-secondary">Agenda registrada para esta fecha</p></div><span class="badge text-bg-success align-self-start"><%= citas.size() %></span></div>
        <% if (citas.isEmpty()) { %><div class="alert alert-light border mb-0">No hay citas registradas para esta fecha.</div><% } else { %>
        <div class="table-responsive"><table class="table align-middle"><thead><tr><th>Hora</th><th>Paciente</th><th>Médico</th><th>Especialidad</th><th>Estado</th><th>Acción</th></tr></thead><tbody>
        <% for (Cita cita : citas) {
            Paciente paciente = pacientesPorId.get(cita.getIdPaciente());
            Medico medico = medicos.get(cita.getIdMedico());
            Especialidad esp = especialidades.get(cita.getIdEspecialidad());
        %><tr><td><strong><%= cita.getFechaHoraInicio().atZoneSameInstant(DisponibilidadService.ZONA_HORARIA).format(hora) %></strong><br><small><%= cita.getFechaHoraFin().atZoneSameInstant(DisponibilidadService.ZONA_HORARIA).format(hora) %></small></td>
            <td><%= paciente == null ? "Paciente" : h(paciente.getNombres() + " " + paciente.getApellidos()) %></td>
            <td><%= medico == null ? "Médico" : h(medico.getNombres() + " " + medico.getApellidos()) %></td>
            <td><%= esp == null ? "Especialidad" : h(esp.getNombre()) %></td>
            <td><span class="badge <%= "CANCELADA".equals(cita.getCodigoEstado()) ? "text-bg-secondary" : "text-bg-warning" %>"><%= h(cita.getNombreEstado()) %></span></td>
            <td><% if (!"CANCELADA".equals(cita.getCodigoEstado()) && !"ATENDIDA".equals(cita.getCodigoEstado())) { %>
                <form method="post" action="${pageContext.request.contextPath}/agenda/citas" class="d-flex gap-2">
                    <input type="hidden" name="accion" value="cancelar"><input type="hidden" name="fecha" value="<%= fecha %>"><input type="hidden" name="idCita" value="<%= cita.getIdCita() %>">
                    <input class="form-control form-control-sm" name="motivoCancelacion" required maxlength="500" placeholder="Motivo"><button class="btn btn-sm btn-outline-danger">Cancelar</button>
                </form><% } %></td></tr><% } %>
        </tbody></table></div><% } %>
    </div></section>
</main>
</body>
</html>
