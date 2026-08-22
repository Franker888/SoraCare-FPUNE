<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.RolAdministrativoDTO" %>
<%@ page import="java.util.List" %>
<%!
    private String h(String valor) { if (valor == null) return ""; return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
    private boolean tiene(String rol, String permiso) {
        if ("admin".equals(permiso)) return "ADMINISTRADOR".equals(rol);
        if ("agenda".equals(permiso)) return "ADMINISTRADOR".equals(rol) || "RECEPCIONISTA".equals(rol);
        if ("pagos".equals(permiso)) return "ADMINISTRADOR".equals(rol) || "RECEPCIONISTA".equals(rol) || "PACIENTE".equals(rol);
        if ("clinica".equals(permiso)) return "MEDICO".equals(rol);
        if ("propio".equals(permiso)) return "PACIENTE".equals(rol);
        return false;
    }
%>
<%
    List<RolAdministrativoDTO> roles = (List<RolAdministrativoDTO>) request.getAttribute("roles");
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html><html lang="es"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="robots" content="noindex,nofollow"><title>Roles y accesos | SoraCare-FPUNE</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css"><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815b"></head>
<body class="app-shell app-admin"><%@ include file="_navbar.jspf" %><main class="container py-4 py-lg-5">
<section class="admin-hero mb-4"><div class="row align-items-center"><div class="col-lg-9"><div class="admin-eyebrow">Etapa 8 · Perfiles protegidos</div><h1>Roles y matriz de acceso</h1><p>Visualizá qué funciones corresponden a cada perfil. Los permisos estructurales permanecen protegidos para evitar accesos clínicos indebidos.</p></div><div class="col-lg-3 text-lg-end"><span class="admin-hero-icon"><i class="bi bi-person-lock"></i></span></div></div></section>
<% if (error != null) { %><div class="alert alert-danger"><%= h(error) %></div><% } %>
<div class="admin-section-heading"><div><h2 class="h4">Perfiles disponibles</h2><p class="text-secondary mb-0">Definidos desde la etapa de seguridad y aplicados por filtros de autorización.</p></div><div class="d-flex gap-2"><a class="btn btn-outline-primary" href="${pageContext.request.contextPath}/admin/usuarios">Gestionar usuarios</a><a class="btn btn-outline-primary" href="${pageContext.request.contextPath}/admin/inicio">Volver</a></div></div>
<section class="row g-4 mb-5"><% if (roles != null) for (RolAdministrativoDTO rol : roles) { %><div class="col-12 col-md-6 col-xl-3"><article class="card admin-module-card module-security d-flex flex-column"><span class="admin-module-icon"><i class="bi bi-shield-lock"></i></span><h3><%= h(rol.nombre()) %></h3><p><%= h(rol.descripcion()) %></p><div class="d-flex justify-content-between align-items-center mt-auto"><span class="badge <%= rol.activo() ? "text-bg-success" : "text-bg-secondary" %>"><%= rol.activo() ? "Activo" : "Inactivo" %></span><span class="text-secondary small"><strong><%= rol.usuariosAsignados() %></strong> cuentas</span></div></article></div><% } %></section>
<section class="card admin-table-card"><div class="admin-section-heading"><div><h2 class="h5">Matriz de permisos</h2><p class="text-secondary mb-0">Resumen de las rutas autorizadas actualmente por el sistema.</p></div><span class="badge text-bg-light"><i class="bi bi-lock me-1"></i>Solo lectura</span></div><div class="table-responsive"><table class="table align-middle text-center"><thead><tr><th class="text-start">Rol</th><th>Administración</th><th>Agenda</th><th>Pagos</th><th>Historia clínica</th><th>Portal propio</th></tr></thead><tbody><% if (roles != null) for (RolAdministrativoDTO rol : roles) { %><tr><td class="text-start"><strong><%= h(rol.nombre()) %></strong></td><% for (String permiso : new String[]{"admin","agenda","pagos","clinica","propio"}) { %><td><% if (tiene(rol.nombre(), permiso)) { %><i class="bi bi-check-circle-fill text-success" aria-label="Permitido"></i><% } else { %><i class="bi bi-dash-circle text-secondary" aria-label="No permitido"></i><% } %></td><% } %></tr><% } %></tbody></table></div><div class="alert alert-primary mt-4 mb-0"><i class="bi bi-info-circle me-2"></i>La asignación de un rol se realiza al crear la cuenta. Cambiar permisos estructurales requiere revisión técnica y de seguridad, por eso no se edita desde esta pantalla.</div></section>
</main><script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script></body></html>
