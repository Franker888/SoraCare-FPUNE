<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.UsuarioAutenticadoDTO" %>
<%@ page import="py.fpune.soracare.dto.ResumenPanelAdministracionDTO" %>
<%@ page import="java.util.Set" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    UsuarioAutenticadoDTO usuario = (UsuarioAutenticadoDTO) request.getAttribute("usuarioAutenticado");
    if (usuario == null) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No existe un usuario autenticado.");
        return;
    }
    ResumenPanelAdministracionDTO resumen =
            (ResumenPanelAdministracionDTO) request.getAttribute("resumenAdministracion");
    Set<String> roles = usuario.getRoles();
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title>Centro administrativo | SoraCare-FPUNE</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815b">
</head>
<body class="app-shell app-admin">
<%@ include file="_navbar.jspf" %>
<main class="container py-4 py-lg-5">
    <section class="admin-hero mb-4">
        <div class="row align-items-center">
            <div class="col-lg-9">
                <div class="admin-eyebrow">Centro de control · Etapa 8</div>
                <h1>Administración de SoraCare</h1>
                <p>Gestioná la operación médica, la seguridad, las configuraciones y los informes desde un único lugar.</p>
                <div class="d-flex flex-wrap gap-2 mt-4"><span class="badge bg-white text-primary px-3 py-2">@<%= h(usuario.getNombreUsuario()) %></span><% for (String rol : roles) { %><span class="badge bg-white bg-opacity-10 border border-white border-opacity-25 px-3 py-2"><%= h(rol) %></span><% } %></div>
            </div>
            <div class="col-lg-3 text-lg-end"><span class="admin-hero-icon"><i class="bi bi-grid-1x2"></i></span></div>
        </div>
    </section>

    <% if (resumen != null) { %>
    <section class="row g-3 mb-5">
        <div class="col-6 col-lg-3"><div class="card admin-stat-card"><i class="bi bi-people admin-stat-icon"></i><div class="admin-stat-value"><%= resumen.usuarios() %></div><div class="admin-stat-label">Usuarios registrados<% if (resumen.usuariosBloqueados() > 0) { %> · <span class="text-warning"><%= resumen.usuariosBloqueados() %> bloqueados</span><% } %></div></div></div>
        <div class="col-6 col-lg-3"><div class="card admin-stat-card"><i class="bi bi-person-hearts admin-stat-icon"></i><div class="admin-stat-value"><%= resumen.pacientes() %></div><div class="admin-stat-label">Pacientes con historia clínica</div></div></div>
        <div class="col-6 col-lg-3"><div class="card admin-stat-card"><i class="bi bi-calendar2-check admin-stat-icon"></i><div class="admin-stat-value"><%= resumen.citasFuturas() %></div><div class="admin-stat-label">Citas futuras activas</div></div></div>
        <div class="col-6 col-lg-3"><div class="card admin-stat-card"><i class="bi bi-shield-check admin-stat-icon"></i><div class="admin-stat-value"><%= resumen.eventosHoy() %></div><div class="admin-stat-label">Operaciones auditadas hoy</div></div></div>
    </section>
    <% } %>

    <section class="mb-5">
        <div class="admin-section-heading"><div><h2 class="h4">Operación hospitalaria</h2><p class="text-secondary mb-0">Catálogos, profesionales, espacios y agenda médica.</p></div><span class="badge text-bg-success">Módulos operativos</span></div>
        <div class="row g-4">
            <div class="col-12 col-md-6 col-xl-4"><article class="card admin-module-card module-clinical d-flex flex-column"><span class="admin-module-icon"><i class="bi bi-heart-pulse"></i></span><h3>Especialidades médicas</h3><p>Organizá los servicios médicos y la duración sugerida de sus consultas.</p><a class="btn btn-outline-primary align-self-start" href="${pageContext.request.contextPath}/admin/especialidades">Abrir especialidades</a></article></div>
            <div class="col-12 col-md-6 col-xl-4"><article class="card admin-module-card module-clinical d-flex flex-column"><span class="admin-module-icon"><i class="bi bi-person-badge"></i></span><h3>Médicos</h3><p>Registrá profesionales, datos públicos y especialidades habilitadas.</p><a class="btn btn-outline-primary align-self-start" href="${pageContext.request.contextPath}/admin/medicos">Abrir médicos</a></article></div>
            <div class="col-12 col-md-6 col-xl-4"><article class="card admin-module-card module-clinical d-flex flex-column"><span class="admin-module-icon"><i class="bi bi-clock-history"></i></span><h3>Horarios y excepciones</h3><p>Configurá agendas semanales, consultorios y bloqueos extraordinarios.</p><div class="d-flex flex-wrap gap-2"><a class="btn btn-outline-primary" href="${pageContext.request.contextPath}/admin/horarios">Horarios</a><a class="btn btn-outline-primary" href="${pageContext.request.contextPath}/admin/excepciones-horarios">Excepciones</a></div></article></div>
            <div class="col-12 col-md-6 col-xl-4"><article class="card admin-module-card d-flex flex-column"><span class="admin-module-icon"><i class="bi bi-hospital"></i></span><h3>Sedes hospitalarias</h3><p>Gestioná las ubicaciones físicas donde opera la institución.</p><a class="btn btn-outline-primary align-self-start" href="${pageContext.request.contextPath}/admin/sedes">Abrir sedes</a></article></div>
            <div class="col-12 col-md-6 col-xl-4"><article class="card admin-module-card d-flex flex-column"><span class="admin-module-icon"><i class="bi bi-door-open"></i></span><h3>Consultorios</h3><p>Administrá los espacios de atención disponibles en cada sede.</p><a class="btn btn-outline-primary align-self-start" href="${pageContext.request.contextPath}/admin/consultorios">Abrir consultorios</a></article></div>
            <div class="col-12 col-md-6 col-xl-4"><article class="card admin-module-card module-clinical d-flex flex-column"><span class="admin-module-icon"><i class="bi bi-capsule-pill"></i></span><h3>Medicamentos</h3><p>Mantené el catálogo farmacológico usado en las indicaciones médicas.</p><a class="btn btn-outline-primary align-self-start" href="${pageContext.request.contextPath}/admin/medicamentos">Abrir medicamentos</a></article></div>
        </div>
    </section>

    <section>
        <div class="admin-section-heading"><div><h2 class="h4">Seguridad, control y análisis</h2><p class="text-secondary mb-0">Módulos finales de administración y preparación del sistema.</p></div><span class="badge text-bg-primary">Etapa 8</span></div>
        <div class="row g-4">
            <div class="col-12 col-md-6 col-xl-4"><article class="card admin-module-card module-security d-flex flex-column"><span class="admin-module-icon"><i class="bi bi-people"></i></span><h3>Usuarios</h3><p>Consultá cuentas, bloqueos, accesos y estados de seguridad.</p><a class="btn btn-primary align-self-start" href="${pageContext.request.contextPath}/admin/usuarios">Gestionar usuarios</a></article></div>
            <div class="col-12 col-md-6 col-xl-4"><article class="card admin-module-card module-security d-flex flex-column"><span class="admin-module-icon"><i class="bi bi-person-lock"></i></span><h3>Roles y accesos</h3><p>Revisá los perfiles habilitados y su matriz de permisos.</p><a class="btn btn-primary align-self-start" href="${pageContext.request.contextPath}/admin/roles">Ver roles</a></article></div>
            <div class="col-12 col-md-6 col-xl-4"><article class="card admin-module-card module-security d-flex flex-column"><span class="admin-module-icon"><i class="bi bi-shield-check"></i></span><h3>Auditoría</h3><p>Investigá operaciones, responsables, rutas y resultados.</p><a class="btn btn-primary align-self-start" href="${pageContext.request.contextPath}/admin/auditoria">Abrir auditoría</a></article></div>
            <div class="col-12 col-md-6 col-xl-4"><article class="card admin-module-card d-flex flex-column"><span class="admin-module-icon"><i class="bi bi-sliders2"></i></span><h3>Configuración</h3><p>Ajustá parámetros con historial y motivo obligatorio.</p><a class="btn btn-primary align-self-start" href="${pageContext.request.contextPath}/admin/configuracion">Abrir configuración</a></article></div>
            <div class="col-12 col-md-6 col-xl-4"><article class="card admin-module-card module-analysis d-flex flex-column"><span class="admin-module-icon"><i class="bi bi-graph-up-arrow"></i></span><h3>Informes y demanda</h3><p>Analizá citas, horarios de concentración, pagos y crecimiento.</p><a class="btn btn-primary align-self-start" href="${pageContext.request.contextPath}/admin/informes">Abrir informes</a></article></div>
            <div class="col-12 col-md-6 col-xl-4"><article class="card admin-module-card module-analysis d-flex flex-column"><span class="admin-module-icon"><i class="bi bi-activity"></i></span><h3>Estado del sistema</h3><p>Verificá PostgreSQL, conexiones, memoria y actividad técnica.</p><a class="btn btn-primary align-self-start" href="${pageContext.request.contextPath}/admin/estado-sistema">Comprobar estado</a></article></div>
            <div class="col-12 col-md-6 col-xl-4"><article class="card admin-module-card module-analysis d-flex flex-column"><span class="admin-module-icon"><i class="bi bi-envelope-check"></i></span><h3>Correo electrónico</h3><p>Verificá Resend antes de enviar comprobantes e indicaciones médicas.</p><a class="btn btn-primary align-self-start" href="${pageContext.request.contextPath}/admin/integraciones/correo">Configurar correo</a></article></div>
            <div class="col-12 col-md-6 col-xl-4"><article class="card admin-module-card module-analysis d-flex flex-column"><span class="admin-module-icon"><i class="bi bi-chat-square-text"></i></span><h3>Mensajes SMS</h3><p>Conectá Twilio para probar recordatorios y confirmaciones de asistencia.</p><a class="btn btn-primary align-self-start" href="${pageContext.request.contextPath}/admin/integraciones/sms">Configurar SMS</a></article></div>
            <div class="col-12 col-md-6 col-xl-4"><article class="card admin-module-card module-security d-flex flex-column"><span class="admin-module-icon"><i class="bi bi-bug"></i></span><h3>Monitoreo de errores</h3><p>Conectá Sentry para detectar fallos técnicos sin enviar datos personales o clínicos.</p><a class="btn btn-primary align-self-start" href="${pageContext.request.contextPath}/admin/integraciones/sentry">Configurar Sentry</a></article></div>
        </div>
    </section>
</main>
<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
</body>
</html>
