<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.ConfiguracionSistemaDTO" %>
<%@ page import="py.fpune.soracare.dto.HistorialConfiguracionDTO" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    private String titulo(String valor) {
        if (valor == null || valor.isBlank()) return "General";
        String limpio = valor.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(limpio.charAt(0)) + limpio.substring(1);
    }
%>
<%
    List<ConfiguracionSistemaDTO> configuraciones = (List<ConfiguracionSistemaDTO>) request.getAttribute("configuraciones");
    List<HistorialConfiguracionDTO> historial = (List<HistorialConfiguracionDTO>) request.getAttribute("historial");
    String error = (String) request.getAttribute("error");
    boolean exito = "actualizada".equals(request.getParameter("resultado"));
    DateTimeFormatter fechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
%>
<!DOCTYPE html><html lang="es"><head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="robots" content="noindex,nofollow">
    <title>Configuración | SoraCare-FPUNE</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815b">
</head><body class="app-shell app-admin">
<%@ include file="_navbar.jspf" %>
<main class="container py-4 py-lg-5">
    <section class="admin-hero mb-4"><div class="row align-items-center"><div class="col-lg-9"><div class="admin-eyebrow">Etapa 8 · Parámetros controlados</div><h1>Configuración del sistema</h1><p>Ajustá políticas operativas e institucionales. Cada cambio exige un motivo y queda registrado automáticamente.</p></div><div class="col-lg-3 text-lg-end"><span class="admin-hero-icon"><i class="bi bi-sliders2"></i></span></div></div></section>
    <% if (exito) { %><div class="alert alert-success"><i class="bi bi-check-circle me-2"></i>La configuración se actualizó y el cambio quedó registrado.</div><% } %>
    <% if (error != null) { %><div class="alert alert-danger"><%= h(error) %></div><% } %>
    <div class="admin-section-heading"><div><h2 class="h4">Parámetros activos</h2><p class="text-secondary mb-0">Modificá solamente los valores necesarios para la operación.</p></div><a class="btn btn-outline-primary" href="${pageContext.request.contextPath}/admin/inicio">Volver a administración</a></div>
    <section class="row g-4 mb-5">
    <% if (configuraciones != null) for (ConfiguracionSistemaDTO configuracion : configuraciones) { %>
        <div class="col-12 col-lg-6"><article class="card admin-content-card h-100">
            <div class="d-flex justify-content-between gap-3 mb-3"><div><span class="badge text-bg-light mb-2"><%= h(titulo(configuracion.categoria())) %></span><h3 class="h5 mb-1"><%= h(configuracion.nombre()) %></h3><span class="admin-code"><%= h(configuracion.clave()) %></span></div><i class="bi bi-gear-wide-connected fs-3 text-primary"></i></div>
            <p class="text-secondary"><%= h(configuracion.descripcion()) %></p>
            <form method="post" class="mt-auto"><input type="hidden" name="csrfToken" value="${requestScope.csrfToken}">
                <input type="hidden" name="idConfiguracion" value="<%= configuracion.idConfiguracion() %>">
                <input type="hidden" name="tipoDato" value="<%= h(configuracion.tipoDato()) %>">
                <label class="form-label">Valor actual</label>
                <% if ("BOOLEANO".equals(configuracion.tipoDato())) { %><select class="form-select mb-3" name="valor" <%= configuracion.editable() ? "" : "disabled" %>><option value="true" <%= "true".equalsIgnoreCase(configuracion.valor()) ? "selected" : "" %>>Habilitado</option><option value="false" <%= "false".equalsIgnoreCase(configuracion.valor()) ? "selected" : "" %>>Deshabilitado</option></select>
                <% } else { %><input class="form-control mb-3" name="valor" maxlength="2000" value="<%= h(configuracion.valor()) %>" <%= configuracion.editable() ? "" : "readonly" %>><% } %>
                <% if (configuracion.editable()) { %><label class="form-label">Motivo del cambio</label><div class="input-group"><input class="form-control" name="motivo" minlength="5" maxlength="500" required placeholder="Ej.: ajuste de política institucional"><button class="btn btn-primary" type="submit">Guardar</button></div><% } else { %><span class="badge text-bg-secondary">Solo lectura</span><% } %>
            </form>
        </article></div>
    <% } %>
    </section>
    <section class="card admin-table-card"><div class="admin-section-heading"><div><h2 class="h5">Historial reciente</h2><p class="text-secondary mb-0">Los últimos cambios con su responsable y justificación.</p></div></div>
        <% if (historial == null || historial.isEmpty()) { %><div class="admin-empty"><i class="bi bi-clock-history"></i>Todavía no se modificaron configuraciones.</div><% } else { %><div class="table-responsive"><table class="table align-middle"><thead><tr><th>Fecha</th><th>Configuración</th><th>Cambio</th><th>Responsable</th><th>Motivo</th></tr></thead><tbody><% for (HistorialConfiguracionDTO item : historial) { %><tr><td><strong><%= item.fechaCambio() == null ? "—" : item.fechaCambio().format(fechaHora) %></strong></td><td><%= h(item.nombreConfiguracion()) %><br><span class="admin-code"><%= h(item.clave()) %></span></td><td><span class="text-secondary"><%= h(item.valorAnterior()) %></span> <i class="bi bi-arrow-right"></i> <strong><%= h(item.valorNuevo()) %></strong></td><td>@<%= h(item.usuario()) %></td><td><%= h(item.motivo()) %></td></tr><% } %></tbody></table></div><% } %>
    </section>
</main><script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script></body></html>
