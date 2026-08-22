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
            request.getAttribute("twilioConfigurado"));
    String remitente = (String) request.getAttribute("remitenteTwilio");
    String mensajeExito = (String) request.getAttribute("mensajeExito");
    String error = (String) request.getAttribute("error");
    String destinatario = (String) request.getAttribute("destinatario");
    String mensaje = (String) request.getAttribute("mensaje");
    if (mensaje == null) {
        mensaje = "SoraCare-FPUNE: esta es una prueba de recordatorio de cita médica.";
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title>Mensajes SMS | SoraCare-FPUNE</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260820d">
</head>
<body class="app-shell app-admin">
<%@ include file="_navbar.jspf" %>
<main class="container py-4 py-lg-5">
    <section class="admin-hero mb-4">
        <div class="row align-items-center">
            <div class="col-lg-9">
                <div class="admin-eyebrow">Integraciones · Mensajería clínica</div>
                <h1>Twilio SMS en SoraCare</h1>
                <p>Prepará recordatorios de citas y confirmaciones de asistencia mediante mensajes de texto.</p>
            </div>
            <div class="col-lg-3 text-lg-end">
                <span class="admin-hero-icon"><i class="bi bi-chat-square-text"></i></span>
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
                    <span class="admin-module-icon"><i class="bi bi-phone"></i></span>
                    <% if (configurado) { %>
                    <span class="badge text-bg-success">API configurada</span>
                    <% } else { %>
                    <span class="badge text-bg-warning">Falta configurar</span>
                    <% } %>
                </div>
                <h2 class="h4">Estado de la integración</h2>
                <p class="text-secondary">El Account SID y el Auth Token se leen desde variables de entorno y nunca se muestran en esta pantalla.</p>
                <dl class="row mb-0">
                    <dt class="col-sm-4">Proveedor</dt><dd class="col-sm-8">Twilio</dd>
                    <dt class="col-sm-4">Canal</dt><dd class="col-sm-8">SMS</dd>
                    <dt class="col-sm-4">Remitente</dt><dd class="col-sm-8"><%= h(remitente) %></dd>
                </dl>
            </article>
        </div>

        <div class="col-lg-7">
            <article class="card admin-module-card h-100">
                <span class="admin-module-icon mb-3"><i class="bi bi-send"></i></span>
                <h2 class="h4">Enviar SMS de prueba</h2>
                <p class="text-secondary">Durante la prueba de Twilio, el teléfono destinatario debe estar verificado y escribirse con código de país.</p>
                <form method="post" action="${pageContext.request.contextPath}/admin/integraciones/sms">
                    <label for="destinatario" class="form-label fw-semibold">Teléfono destinatario</label>
                    <input class="form-control form-control-lg mb-3" type="tel"
                           id="destinatario" name="destinatario" maxlength="16"
                           required value="<%= h(destinatario) %>"
                           placeholder="+595981234567">

                    <label for="mensaje" class="form-label fw-semibold">Mensaje de prueba</label>
                    <textarea class="form-control mb-3" id="mensaje" name="mensaje"
                              maxlength="1000" rows="4" required><%= h(mensaje) %></textarea>

                    <button class="btn btn-primary" type="submit"
                            <%= configurado ? "" : "disabled" %>>
                        <i class="bi bi-send me-2"></i>Enviar prueba
                    </button>
                    <a class="btn btn-outline-primary ms-2"
                       href="${pageContext.request.contextPath}/admin/inicio">Volver a administración</a>
                </form>
            </article>
        </div>
    </section>

    <section class="card admin-module-card mt-4">
        <h2 class="h5">Próxima automatización</h2>
        <p class="mb-0 text-secondary">Después de validar el primer SMS, SoraCare enviará recordatorios 24 horas antes y registrará el resultado sin bloquear la cita si el proveedor no responde.</p>
    </section>
</main>
<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
</body>
</html>
