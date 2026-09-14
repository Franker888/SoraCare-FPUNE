<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    boolean configurado = Boolean.TRUE.equals(
            request.getAttribute("sentryConfigurado"));
    boolean activo = Boolean.TRUE.equals(
            request.getAttribute("sentryActivo"));
    String ambiente = (String) request.getAttribute("sentryAmbiente");
    String mensajeExito = (String) request.getAttribute("mensajeExito");
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title>Monitoreo de errores | SoraCare-FPUNE</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260824a">
</head>
<body class="app-shell app-admin">
<%@ include file="_navbar.jspf" %>
<main class="container py-4 py-lg-5">
    <section class="admin-hero mb-4">
        <div class="row align-items-center">
            <div class="col-lg-9">
                <div class="admin-eyebrow">Integraciones · Observabilidad segura</div>
                <h1>Sentry en SoraCare</h1>
                <p>Detectá fallos técnicos del servidor sin incorporar identidades, sesiones ni información clínica.</p>
            </div>
            <div class="col-lg-3 text-lg-end">
                <span class="admin-hero-icon"><i class="bi bi-bug"></i></span>
            </div>
        </div>
    </section>

    <% if (mensajeExito != null) { %>
    <div class="alert alert-success" role="alert"><%= h(mensajeExito) %></div>
    <% } %>
    <% if (error != null) { %>
    <div class="alert alert-danger" role="alert"><%= h(error) %></div>
    <% } %>

    <section class="row g-4">
        <div class="col-lg-5">
            <article class="card admin-module-card h-100">
                <div class="d-flex align-items-start justify-content-between gap-3 mb-3">
                    <span class="admin-module-icon"><i class="bi bi-activity"></i></span>
                    <% if (activo) { %>
                    <span class="badge text-bg-success">Monitoreo activo</span>
                    <% } else if (configurado) { %>
                    <span class="badge text-bg-danger">Configuración inválida</span>
                    <% } else { %>
                    <span class="badge text-bg-warning">Falta configurar</span>
                    <% } %>
                </div>
                <h2 class="h4">Estado de la integración</h2>
                <p class="text-secondary">El DSN se lee desde una variable de entorno del servidor y nunca se muestra en pantalla.</p>
                <dl class="row mb-0">
                    <dt class="col-sm-5">Proveedor</dt><dd class="col-sm-7">Sentry</dd>
                    <dt class="col-sm-5">Ambiente</dt><dd class="col-sm-7"><%= h(ambiente) %></dd>
                    <dt class="col-sm-5">Datos personales</dt><dd class="col-sm-7">Desactivados</dd>
                    <dt class="col-sm-5">Trazas de rendimiento</dt><dd class="col-sm-7">Desactivadas</dd>
                </dl>
            </article>
        </div>

        <div class="col-lg-7">
            <article class="card admin-module-card h-100">
                <span class="admin-module-icon mb-3"><i class="bi bi-send-check"></i></span>
                <h2 class="h4">Enviar evento de prueba</h2>
                <p class="text-secondary">Genera un error controlado y anonimizado para comprobar que el proyecto de Sentry puede recibir eventos.</p>
                <form method="post" action="${pageContext.request.contextPath}/admin/integraciones/sentry"><input type="hidden" name="csrfToken" value="${requestScope.csrfToken}">
                    <button class="btn btn-primary" type="submit"
                            <%= activo ? "" : "disabled" %>>
                        <i class="bi bi-bug me-2"></i>Enviar prueba segura
                    </button>
                    <a class="btn btn-outline-primary ms-2"
                       href="${pageContext.request.contextPath}/admin/inicio">Volver a administración</a>
                </form>
            </article>
        </div>
    </section>

    <section class="card admin-module-card mt-4">
        <h2 class="h5">Privacidad del monitoreo</h2>
        <p class="mb-0 text-secondary">SoraCare envía únicamente el tipo técnico de error, una ruta normalizada, el método HTTP y la pila de ejecución. No envía nombres, correos, cédulas, IP, cookies, parámetros, contenido de formularios, contraseñas ni datos médicos.</p>
    </section>
</main>
<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
</body>
</html>
