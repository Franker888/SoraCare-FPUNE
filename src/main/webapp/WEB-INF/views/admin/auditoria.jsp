<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.EventoAuditoriaDTO" %>
<%@ page import="py.fpune.soracare.dto.FiltroAuditoriaDTO" %>
<%@ page import="py.fpune.soracare.dto.ResumenAuditoriaDTO" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    private String etiqueta(String valor) {
        if (valor == null) return "";
        return valor.toLowerCase().replace('_', ' ');
    }
%>
<%
    FiltroAuditoriaDTO filtro = (FiltroAuditoriaDTO) request.getAttribute("filtro");
    ResumenAuditoriaDTO resumen = (ResumenAuditoriaDTO) request.getAttribute("resumen");
    String error = (String) request.getAttribute("error");
    DateTimeFormatter fechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title>Auditoría | SoraCare-FPUNE</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815b">
</head>
<body class="app-shell app-admin">
<%@ include file="_navbar.jspf" %>
<main class="container py-4 py-lg-5">
    <section class="admin-hero mb-4">
        <div class="row align-items-center">
            <div class="col-lg-9">
                <div class="admin-eyebrow">Etapa 8 · Seguridad y trazabilidad</div>
                <h1>Auditoría del sistema</h1>
                <p>Revisá quién realizó cada operación, cuándo ocurrió y cuál fue su resultado. Esta bitácora es inmutable.</p>
            </div>
            <div class="col-lg-3 text-lg-end"><span class="admin-hero-icon"><i class="bi bi-shield-check"></i></span></div>
        </div>
    </section>

    <% if (error != null) { %><div class="alert alert-danger"><%= h(error) %></div><% } %>

    <% if (resumen != null) { %>
    <section class="row g-3 mb-4">
        <div class="col-6 col-lg-3"><div class="card admin-stat-card"><i class="bi bi-journal-check admin-stat-icon"></i><div class="admin-stat-value"><%= resumen.total() %></div><div class="admin-stat-label">Eventos del período</div></div></div>
        <div class="col-6 col-lg-3"><div class="card admin-stat-card"><i class="bi bi-check-circle admin-stat-icon"></i><div class="admin-stat-value text-success"><%= resumen.exitosos() %></div><div class="admin-stat-label">Operaciones exitosas</div></div></div>
        <div class="col-6 col-lg-3"><div class="card admin-stat-card"><i class="bi bi-exclamation-triangle admin-stat-icon"></i><div class="admin-stat-value text-danger"><%= resumen.fallidos() %></div><div class="admin-stat-label">Operaciones fallidas</div></div></div>
        <div class="col-6 col-lg-3"><div class="card admin-stat-card"><i class="bi bi-people admin-stat-icon"></i><div class="admin-stat-value"><%= resumen.usuariosDistintos() %></div><div class="admin-stat-label">Usuarios identificados</div></div></div>
    </section>
    <% } %>

    <section class="card admin-filter-card mb-4">
        <div class="admin-section-heading"><div><h2 class="h5">Filtros de consulta</h2><p class="text-secondary mb-0">Se muestran como máximo los 150 eventos más recientes.</p></div><a class="btn btn-outline-primary" href="${pageContext.request.contextPath}/admin/inicio">Volver a administración</a></div>
        <form method="get" class="row g-3">
            <div class="col-6 col-lg-2"><label class="form-label">Desde</label><input type="date" class="form-control" name="desde" value="<%= filtro == null ? "" : filtro.desde() %>"></div>
            <div class="col-6 col-lg-2"><label class="form-label">Hasta</label><input type="date" class="form-control" name="hasta" value="<%= filtro == null ? "" : filtro.hasta() %>"></div>
            <div class="col-md-4 col-lg-2"><label class="form-label">Evento</label><select class="form-select" name="tipo"><option value="">Todos</option><% for (String tipo : new String[]{"CREACION","MODIFICACION","ELIMINACION","INICIO_SESION","CIERRE_SESION","AUTENTICACION_FALLIDA","ERROR"}) { %><option value="<%= tipo %>" <%= filtro != null && tipo.equals(filtro.tipoEvento()) ? "selected" : "" %>><%= h(etiqueta(tipo)) %></option><% } %></select></div>
            <div class="col-md-4 col-lg-2"><label class="form-label">Resultado</label><select class="form-select" name="resultado"><option value="">Todos</option><option value="EXITOSO" <%= filtro != null && "EXITOSO".equals(filtro.resultado()) ? "selected" : "" %>>Exitoso</option><option value="FALLIDO" <%= filtro != null && "FALLIDO".equals(filtro.resultado()) ? "selected" : "" %>>Fallido</option></select></div>
            <div class="col-md-4 col-lg-2"><label class="form-label">Módulo</label><select class="form-select" name="modulo"><option value="">Todos</option><% if (resumen != null) for (String modulo : resumen.modulos()) { %><option value="<%= h(modulo) %>" <%= filtro != null && modulo.equals(filtro.modulo()) ? "selected" : "" %>><%= h(etiqueta(modulo)) %></option><% } %></select></div>
            <div class="col-lg-2"><label class="form-label">Buscar</label><input class="form-control" name="q" maxlength="100" placeholder="Usuario, ruta..." value="<%= filtro == null ? "" : h(filtro.busqueda()) %>"></div>
            <div class="col-12 d-flex justify-content-end"><button class="btn btn-primary px-4"><i class="bi bi-funnel me-1"></i> Aplicar filtros</button></div>
        </form>
    </section>

    <section class="card admin-table-card">
        <div class="admin-section-heading"><div><h2 class="h5">Registro de actividad</h2><p class="text-secondary mb-0">Las contraseñas y contenidos clínicos no se almacenan en esta vista.</p></div></div>
        <% if (resumen == null || resumen.eventos().isEmpty()) { %><div class="admin-empty"><i class="bi bi-inbox"></i>No hay eventos para los filtros elegidos.</div><% } else { %>
        <div class="table-responsive"><table class="table align-middle"><thead><tr><th>Fecha y usuario</th><th>Evento</th><th>Módulo y entidad</th><th>Ruta</th><th>Resultado</th></tr></thead><tbody>
        <% for (EventoAuditoriaDTO evento : resumen.eventos()) { %><tr>
            <td><strong><%= evento.fechaEvento() == null ? "—" : evento.fechaEvento().format(fechaHora) %></strong><br><span class="text-secondary small"><%= h(evento.actor()) %><% if (evento.nombreUsuario() != null) { %> · @<%= h(evento.nombreUsuario()) %><% } %></span></td>
            <td><span class="badge text-bg-light"><%= h(etiqueta(evento.tipoEvento())) %></span><% if (evento.descripcion() != null) { %><div class="small text-secondary mt-2"><%= h(evento.descripcion()) %></div><% } %></td>
            <td><strong><%= h(etiqueta(evento.modulo())) %></strong><br><span class="small text-secondary"><%= h(evento.entidad()) %></span></td>
            <td><span class="admin-code"><%= h(evento.metodoHttp()) %></span><div class="small text-secondary mt-2 text-break"><%= h(evento.rutaSolicitud()) %></div></td>
            <td><span class="badge <%= "EXITOSO".equals(evento.resultado()) ? "text-bg-success" : "text-bg-danger" %>"><%= h(etiqueta(evento.resultado())) %></span><% if (evento.mensajeError() != null) { %><div class="small text-danger mt-2"><%= h(evento.mensajeError()) %></div><% } %></td>
        </tr><% } %></tbody></table></div><% } %>
    </section>
</main>
<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
</body></html>
