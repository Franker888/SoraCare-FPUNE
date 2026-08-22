<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.UsuarioAutenticadoDTO" %>
<%@ page import="py.fpune.soracare.dto.ResumenPanelRecepcionDTO" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    UsuarioAutenticadoDTO usuario =
            (UsuarioAutenticadoDTO) request.getAttribute("usuarioAutenticado");
    if (usuario == null) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No existe un usuario autenticado.");
        return;
    }
    ResumenPanelRecepcionDTO resumen =
            (ResumenPanelRecepcionDTO) request.getAttribute("resumenRecepcion");
    boolean errorResumen = Boolean.TRUE.equals(request.getAttribute("errorResumen"));
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title>Centro de recepci&oacute;n | SoraCare-FPUNE</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815d">
</head>
<body class="app-shell app-reception">
<%@ include file="_navbar.jspf" %>
<main class="container py-4 py-lg-5">
    <section class="reception-hero mb-4">
        <div class="row align-items-center">
            <div class="col-lg-9">
                <div class="reception-eyebrow">Centro operativo</div>
                <h1>Recepci&oacute;n de SoraCare</h1>
                <p>Organiz&aacute pacientes, citas y cobros desde un panel preparado para la atenci&oacute;n diaria.</p>
                <div class="d-flex flex-wrap gap-2 mt-4">
                    <span class="badge bg-white text-primary px-3 py-2">
                        @<%= h(usuario.getNombreUsuario()) %>
                    </span>
                    <span class="badge reception-role-badge px-3 py-2">RECEPCIONISTA</span>
                </div>
            </div>
            <div class="col-lg-3 text-lg-end">
                <span class="reception-hero-icon"><i class="bi bi-headset"></i></span>
            </div>
        </div>
    </section>

    <% if (errorResumen) { %>
        <div class="alert alert-warning mb-4" role="alert">
            <i class="bi bi-exclamation-triangle me-2"></i>
            Las funciones est&aacute;n disponibles, pero no se pudo actualizar el resumen operativo.
        </div>
    <% } %>

    <% if (resumen != null) { %>
    <section class="row g-3 mb-5" aria-label="Resumen operativo de recepción">
        <div class="col-6 col-lg-3">
            <article class="card reception-stat-card">
                <span class="reception-stat-icon"><i class="bi bi-calendar2-day"></i></span>
                <div class="reception-stat-value"><%= resumen.citasHoy() %></div>
                <div class="reception-stat-label">Citas activas de hoy</div>
            </article>
        </div>
        <div class="col-6 col-lg-3">
            <article class="card reception-stat-card">
                <span class="reception-stat-icon stat-confirmed"><i class="bi bi-calendar2-check"></i></span>
                <div class="reception-stat-value"><%= resumen.confirmadasHoy() %></div>
                <div class="reception-stat-label">Confirmadas para hoy</div>
            </article>
        </div>
        <div class="col-6 col-lg-3">
            <article class="card reception-stat-card">
                <span class="reception-stat-icon stat-waiting"><i class="bi bi-hourglass-split"></i></span>
                <div class="reception-stat-value"><%= resumen.porConfirmarHoy() %></div>
                <div class="reception-stat-label">Por confirmar hoy</div>
            </article>
        </div>
        <div class="col-6 col-lg-3">
            <article class="card reception-stat-card">
                <span class="reception-stat-icon stat-payment"><i class="bi bi-wallet2"></i></span>
                <div class="reception-stat-value"><%= resumen.cobrosPendientes() %></div>
                <div class="reception-stat-label">Cobros pendientes</div>
            </article>
        </div>
    </section>
    <% } %>

    <section>
        <div class="reception-section-heading">
            <div>
                <h2 class="h4">Tareas de recepci&oacute;n</h2>
                <p class="text-secondary mb-0">Accesos ordenados seg&uacute;n el recorrido habitual de atenci&oacute;n.</p>
            </div>
            <span class="reception-shift-badge"><i class="bi bi-circle-fill"></i> Operaci&oacute;n activa</span>
        </div>

        <div class="row g-4">
            <div class="col-12 col-md-6 col-xl-3">
                <article class="card reception-module-card module-patients d-flex flex-column">
                    <span class="reception-step">01</span>
                    <span class="reception-module-icon"><i class="bi bi-person-plus"></i></span>
                    <h3>Pacientes</h3>
                    <p>Registr&aacute pacientes, busc&aacute por c&eacute;dula y consult&aacute su informaci&oacute;n disponible.</p>
                    <a class="btn btn-outline-primary align-self-start"
                       href="${pageContext.request.contextPath}/pacientes/registro">
                        Gestionar pacientes <i class="bi bi-arrow-right ms-1"></i>
                    </a>
                </article>
            </div>
            <div class="col-12 col-md-6 col-xl-3">
                <article class="card reception-module-card module-agenda d-flex flex-column">
                    <span class="reception-step">02</span>
                    <span class="reception-module-icon"><i class="bi bi-calendar2-week"></i></span>
                    <h3>Agenda y citas</h3>
                    <p>Consult&aacute turnos, agend&aacute pacientes y administr&aacute la agenda operativa del centro.</p>
                    <a class="btn btn-outline-primary align-self-start"
                       href="${pageContext.request.contextPath}/agenda/citas">
                        Abrir agenda <i class="bi bi-arrow-right ms-1"></i>
                    </a>
                </article>
            </div>
            <div class="col-12 col-md-6 col-xl-3">
                <article class="card reception-module-card module-payments d-flex flex-column">
                    <span class="reception-step">03</span>
                    <span class="reception-module-icon"><i class="bi bi-credit-card-2-front"></i></span>
                    <h3>Pagos y comprobantes</h3>
                    <p>Busc&aacute por c&eacute;dula, confirm&aacute cobros en efectivo y consult&aacute comprobantes.</p>
                    <a class="btn btn-outline-primary align-self-start"
                       href="${pageContext.request.contextPath}/agenda/pagos">
                        Gestionar pagos <i class="bi bi-arrow-right ms-1"></i>
                    </a>
                </article>
            </div>
            <div class="col-12 col-md-6 col-xl-3">
                <article class="card reception-module-card module-availability d-flex flex-column">
                    <span class="reception-step">04</span>
                    <span class="reception-module-icon"><i class="bi bi-calendar2-range"></i></span>
                    <h3>Disponibilidad m&eacute;dica</h3>
                    <p>Busc&aacute; un profesional y consult&aacute; sus pr&oacute;ximos d&iacute;as y horarios libres.</p>
                    <a class="btn btn-outline-primary align-self-start"
                       href="${pageContext.request.contextPath}/recepcion/disponibilidad">
                        Ver disponibilidad <i class="bi bi-arrow-right ms-1"></i>
                    </a>
                </article>
            </div>
        </div>
    </section>
</main>
<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
</body>
</html>
