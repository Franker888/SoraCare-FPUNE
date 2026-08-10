<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.mvc.model.Consultorio" %>
<%@ page import="py.fpune.soracare.mvc.model.Especialidad" %>
<%@ page import="py.fpune.soracare.mvc.model.ExcepcionHorarioMedico" %>
<%@ page import="py.fpune.soracare.mvc.model.Medico" %>
<%@ page import="py.fpune.soracare.mvc.model.MedicoEspecialidad" %>
<%@ page import="java.time.LocalDate" %>
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
    List<ExcepcionHorarioMedico> excepciones = request.getAttribute("excepciones") instanceof List<?>
            ? (List<ExcepcionHorarioMedico>) request.getAttribute("excepciones") : List.of();
    List<Medico> medicos = request.getAttribute("medicos") instanceof List<?>
            ? (List<Medico>) request.getAttribute("medicos") : List.of();
    List<Consultorio> consultorios = request.getAttribute("consultorios") instanceof List<?>
            ? (List<Consultorio>) request.getAttribute("consultorios") : List.of();
    Map<Long, Medico> medicosPorId = (Map<Long, Medico>) request.getAttribute("medicosPorId");
    Map<Integer, Especialidad> especialidadesPorId = (Map<Integer, Especialidad>) request.getAttribute("especialidadesPorId");
    Map<Long, Consultorio> consultoriosPorId = (Map<Long, Consultorio>) request.getAttribute("consultoriosPorId");
    Map<Long, List<MedicoEspecialidad>> relaciones = (Map<Long, List<MedicoEspecialidad>>) request.getAttribute("relacionesPorMedico");
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Excepciones de agenda | SoraCare-FPUNE</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>body{background:#f4f7f6}.cabecera{background:linear-gradient(135deg,#073f32,#0c7658);color:white}.card{border:0}</style>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css">
</head>
<body class="app-shell app-admin">
<nav class="navbar bg-white border-bottom"><div class="container py-2">
    <a class="navbar-brand fw-bold text-success" href="${pageContext.request.contextPath}/dashboard">SoraCare-FPUNE</a>
    <div class="d-flex gap-2"><a class="btn btn-outline-success btn-sm" href="${pageContext.request.contextPath}/admin/horarios">Horarios</a><a class="btn btn-outline-secondary btn-sm" href="${pageContext.request.contextPath}/admin/inicio">Administración</a></div>
</div></nav>
<header class="cabecera py-4"><div class="container"><p class="small text-uppercase mb-1">Agenda médica</p><h1 class="h2">Excepciones de horarios</h1><p class="mb-0">Registrá vacaciones, bloqueos parciales o disponibilidad extraordinaria.</p></div></header>
<main class="container py-4">
    <% if (request.getAttribute("mensajeExito") != null) { %><div class="alert alert-success"><%= h((String) request.getAttribute("mensajeExito")) %></div><% } %>
    <% if (request.getAttribute("error") != null) { %><div class="alert alert-danger"><%= h((String) request.getAttribute("error")) %></div><% } %>
    <section class="card shadow-sm mb-4"><div class="card-body p-4"><h2 class="h4">Nueva excepción</h2>
        <form method="post" action="${pageContext.request.contextPath}/admin/excepciones-horarios" class="row g-3">
            <input type="hidden" name="accion" value="registrar">
            <div class="col-md-6"><label class="form-label" for="medicoEspecialidad">Médico y alcance</label><select class="form-select" id="medicoEspecialidad" name="medicoEspecialidad" required><option value="">Seleccionar</option>
                <% for (Medico medico : medicos) { %><option value="<%= medico.getIdMedico() %>"><%= h(medico.getNombres() + " " + medico.getApellidos()) %> — Todas las especialidades</option>
                    <% for (MedicoEspecialidad relacion : relaciones.getOrDefault(medico.getIdMedico(), List.of())) { Especialidad esp = especialidadesPorId.get(relacion.getIdEspecialidad()); %>
                    <option value="<%= medico.getIdMedico() %>:<%= relacion.getIdEspecialidad() %>"><%= h(medico.getNombres() + " " + medico.getApellidos()) %> — <%= esp == null ? "Especialidad" : h(esp.getNombre()) %></option><% } %>
                <% } %></select></div>
            <div class="col-md-3"><label class="form-label" for="tipo">Tipo</label><select class="form-select" id="tipo" name="tipo" required><option value="BLOQUEO_TOTAL">Bloqueo total</option><option value="BLOQUEO_PARCIAL">Bloqueo parcial</option><option value="DISPONIBILIDAD_EXTRA">Disponibilidad extra</option></select></div>
            <div class="col-md-3"><label class="form-label" for="fecha">Fecha</label><input class="form-control" type="date" id="fecha" name="fecha" required min="<%= LocalDate.now() %>"></div>
            <div class="col-md-4"><label class="form-label" for="idConsultorio">Consultorio (opcional)</label><select class="form-select" id="idConsultorio" name="idConsultorio"><option value="">Todos / sin especificar</option><% for (Consultorio c : consultorios) { %><option value="<%= c.getIdConsultorio() %>"><%= h(c.getCodigo() + " — " + c.getNombre()) %></option><% } %></select></div>
            <div class="col-md-2"><label class="form-label" for="horaInicio">Desde</label><input class="form-control" type="time" id="horaInicio" name="horaInicio"></div>
            <div class="col-md-2"><label class="form-label" for="horaFin">Hasta</label><input class="form-control" type="time" id="horaFin" name="horaFin"></div>
            <div class="col-md-4"><label class="form-label" for="motivo">Motivo</label><input class="form-control" id="motivo" name="motivo" maxlength="255" required placeholder="Vacaciones, licencia, reunión..."></div>
            <div class="col-12"><small class="text-secondary d-block mb-2">Para bloqueo total dejá las horas vacías. La disponibilidad extra exige especialidad, consultorio y horas.</small><button class="btn btn-success">Registrar excepción</button></div>
        </form>
    </div></section>
    <section class="card shadow-sm"><div class="card-body p-4"><div class="d-flex justify-content-between"><h2 class="h4">Excepciones registradas</h2><span class="badge text-bg-success"><%= excepciones.size() %></span></div>
        <% if (excepciones.isEmpty()) { %><div class="alert alert-light border mb-0">No hay excepciones registradas.</div><% } else { %><div class="table-responsive"><table class="table align-middle"><thead><tr><th>Fecha</th><th>Médico</th><th>Alcance</th><th>Tipo y horario</th><th>Motivo</th><th>Estado</th><th>Acción</th></tr></thead><tbody>
        <% for (ExcepcionHorarioMedico e : excepciones) { Medico m = medicosPorId.get(e.getIdMedico()); Especialidad esp = e.getIdEspecialidad() == null ? null : especialidadesPorId.get(e.getIdEspecialidad()); Consultorio c = e.getIdConsultorio() == null ? null : consultoriosPorId.get(e.getIdConsultorio()); %>
        <tr><td><strong><%= e.getFecha() %></strong></td><td><%= m == null ? "Médico" : h(m.getNombres() + " " + m.getApellidos()) %></td><td><%= esp == null ? "Todas las especialidades" : h(esp.getNombre()) %><br><small><%= c == null ? "Todos los consultorios" : h(c.getNombre()) %></small></td><td><%= h(e.getTipo().name().replace('_',' ')) %><% if (e.getHoraInicio() != null) { %><br><small><%= e.getHoraInicio() %> – <%= e.getHoraFin() %></small><% } %></td><td><%= h(e.getMotivo()) %></td><td><span class="badge <%= e.isActivo() ? "text-bg-success" : "text-bg-secondary" %>"><%= e.isActivo() ? "Activa" : "Inactiva" %></span></td><td><form method="post" action="${pageContext.request.contextPath}/admin/excepciones-horarios"><input type="hidden" name="accion" value="cambiarEstado"><input type="hidden" name="idExcepcion" value="<%= e.getIdExcepcion() %>"><input type="hidden" name="activo" value="<%= !e.isActivo() %>"><button class="btn btn-sm <%= e.isActivo() ? "btn-outline-danger" : "btn-outline-success" %>"><%= e.isActivo() ? "Inactivar" : "Activar" %></button></form></td></tr>
        <% } %></tbody></table></div><% } %>
    </div></section>
</main>
</body>
</html>
