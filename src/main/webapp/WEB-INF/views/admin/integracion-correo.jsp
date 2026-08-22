<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    boolean configurado = Boolean.TRUE.equals(request.getAttribute("resendConfigurado"));
    boolean remitentePrueba = Boolean.TRUE.equals(request.getAttribute("remitentePruebaResend"));
    String remitente = (String) request.getAttribute("remitenteResend");
    String mensajeExito = (String) request.getAttribute("mensajeExito");
    String error = (String) request.getAttribute("error");
    String destinatario = (String) request.getAttribute("destinatario");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title>Correo electrónico | SoraCare-FPUNE</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260820c">
</head>
<body class="app-shell app-admin">
<%@ include file="_navbar.jspf" %>
<main class="container py-4 py-lg-5">
    <section class="admin-hero mb-4">
        <div class="row align-items-center">
            <div class="col-lg-9">
                <div class="admin-eyebrow">Integraciones · Correo electrónico</div>
                <h1>Resend en SoraCare</h1>
                <p>Supervisá el envío automático de comprobantes e indicaciones médicas mediante Resend.</p>
            </div>
            <div class="col-lg-3 text-lg-end">
                <span class="admin-hero-icon"><i class="bi bi-envelope-check"></i></span>
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
                    <span class="admin-module-icon"><i class="bi bi-plug"></i></span>
                    <% if (configurado) { %>
                    <span class="badge text-bg-success">API configurada</span>
                    <% } else { %>
                    <span class="badge text-bg-warning">Falta configurar</span>
                    <% } %>
                </div>
                <h2 class="h4">Estado de la integración</h2>
                <p class="text-secondary">La clave se obtiene exclusivamente desde una variable de entorno y nunca se muestra en pantalla.</p>
                <dl class="row mb-0">
                    <dt class="col-sm-4">Proveedor</dt><dd class="col-sm-8">Resend</dd>
                    <dt class="col-sm-4">Remitente</dt><dd class="col-sm-8"><%= h(remitente) %></dd>
                    <dt class="col-sm-4">Modo</dt><dd class="col-sm-8"><%= remitentePrueba ? "Prueba" : "Dominio configurado" %></dd>
                </dl>
            </article>
        </div>

        <div class="col-lg-7">
            <article class="card admin-module-card h-100">
                <span class="admin-module-icon mb-3"><i class="bi bi-send-check"></i></span>
                <h2 class="h4">Enviar correo de prueba</h2>
                <p class="text-secondary">Usá primero el correo asociado a tu cuenta de Resend. El botón se habilita cuando Tomcat recibe la clave.</p>
                <form method="post" action="${pageContext.request.contextPath}/admin/integraciones/correo">
                    <label for="destinatario" class="form-label fw-semibold">Correo destinatario</label>
                    <input class="form-control form-control-lg mb-3" type="email" id="destinatario"
                           name="destinatario" maxlength="254" required
                           value="<%= h(destinatario) %>" placeholder="tu-correo@ejemplo.com">
                    <button class="btn btn-primary" type="submit" <%= configurado ? "" : "disabled" %>>
                        <i class="bi bi-send me-2"></i>Enviar prueba
                    </button>
                    <a class="btn btn-outline-primary ms-2" href="${pageContext.request.contextPath}/admin/inicio">Volver a administración</a>
                </form>
            </article>
        </div>
    </section>

    <section class="card admin-module-card mt-4">
        <h2 class="h5">Automatizaciones habilitadas</h2>
        <p class="mb-0 text-secondary">Los pagos confirmados envían su comprobante PDF y las atenciones finalizadas envían las indicaciones médicas al paciente. Para utilizar destinatarios externos en producción, el dominio propio deberá estar verificado con SPF y DKIM.</p>
    </section>
</main>
<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
</body>
</html>
