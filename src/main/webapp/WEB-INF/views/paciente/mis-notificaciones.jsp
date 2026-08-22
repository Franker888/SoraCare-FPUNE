<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.NotificacionDTO" %>
<%@ page import="py.fpune.soracare.service.DisponibilidadService" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
%>
<%
    List<NotificacionDTO> notificaciones = request.getAttribute("notificaciones") instanceof List<?>
            ? (List<NotificacionDTO>) request.getAttribute("notificaciones") : List.of();
    long noLeidas = notificaciones.stream().filter(n -> !n.estaLeida()).count();
    String mensajeExito = (String) request.getAttribute("mensajeExito");
    String error = (String) request.getAttribute("error");
    DateTimeFormatter fechaHora = DateTimeFormatter.ofPattern(
            "dd/MM/yyyy HH:mm", new Locale("es", "PY"));
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>Mis notificaciones | SoraCare-FPUNE</title>
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap-icons/bootstrap-icons.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815">
    <style>
        body { background: #f4f7fb; }
        .hero { border: 0; border-radius: 1.25rem; color: #fff;
            background: linear-gradient(135deg, #173da8, #2563eb 58%, #13a6a2); }
        .notificacion { border: 1px solid #dce5f1; border-radius: 1rem; }
        .notificacion.nueva { border-left: 5px solid #2563eb; background: #f8fbff; }
        .icono { width: 46px; height: 46px; display: grid; place-items: center;
            flex: 0 0 46px; border-radius: 14px; background: #eaf1ff; color: #2563eb; }
    </style>
</head>
<body class="app-shell app-patient">
<nav class="navbar bg-white border-bottom">
    <div class="container py-2">
        <a class="navbar-brand fw-bold text-primary" href="${pageContext.request.contextPath}/paciente/inicio">SoraCare-FPUNE</a>
        <a class="btn btn-outline-primary btn-sm" href="${pageContext.request.contextPath}/paciente/inicio">Volver al panel</a>
    </div>
</nav>
<main class="container py-4 py-lg-5">
    <section class="card hero shadow-sm mb-4">
        <div class="card-body p-4 p-lg-5 d-flex flex-column flex-lg-row justify-content-between gap-3">
            <div>
                <p class="mb-2 opacity-75">Portal del paciente</p>
                <h1 class="display-6 fw-bold mb-2">Mis notificaciones</h1>
                <p class="mb-0">Novedades importantes sobre tus citas, pagos, indicaciones médicas y documentos.</p>
            </div>
            <div class="align-self-lg-center">
                <span class="badge bg-white text-primary fs-6 px-3 py-2"><%= noLeidas %> sin leer</span>
            </div>
        </div>
    </section>

    <% if (mensajeExito != null) { %><div class="alert alert-success"><%= h(mensajeExito) %></div><% } %>
    <% if (error != null) { %><div class="alert alert-danger"><%= h(error) %></div><% } %>

    <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
            <h2 class="h4 mb-1">Centro de avisos</h2>
            <p class="text-secondary mb-0">Se muestran hasta las 100 notificaciones más recientes.</p>
        </div>
        <% if (noLeidas > 0) { %>
        <form method="post" action="${pageContext.request.contextPath}/mis-notificaciones">
            <input type="hidden" name="accion" value="todas">
            <button class="btn btn-outline-primary">Marcar todas como leídas</button>
        </form>
        <% } %>
    </div>

    <% if (notificaciones.isEmpty()) { %>
    <section class="card notificacion shadow-sm">
        <div class="card-body text-center py-5">
            <div class="icono mx-auto mb-3"><i class="bi bi-bell"></i></div>
            <h2 class="h5">No tenés notificaciones</h2>
            <p class="text-secondary mb-0">Los nuevos avisos aparecerán en esta sección.</p>
        </div>
    </section>
    <% } else { %>
    <div class="d-grid gap-3">
        <% for (NotificacionDTO notificacion : notificaciones) { %>
        <article class="card notificacion shadow-sm <%= notificacion.estaLeida() ? "" : "nueva" %>">
            <div class="card-body p-4 d-flex flex-column flex-md-row gap-3">
                <div class="icono"><i class="bi bi-bell-fill"></i></div>
                <div class="flex-grow-1">
                    <div class="d-flex flex-wrap justify-content-between gap-2 mb-2">
                        <div>
                            <h2 class="h5 mb-1"><%= h(notificacion.asunto()) %></h2>
                            <span class="text-secondary small">
                                <%= notificacion.fechaCreacion().atZoneSameInstant(
                                        DisponibilidadService.ZONA_HORARIA).format(fechaHora) %>
                            </span>
                        </div>
                        <span class="badge <%= notificacion.estaLeida() ? "text-bg-light" : "text-bg-primary" %> align-self-start">
                            <%= notificacion.estaLeida() ? "Leída" : "Nueva" %>
                        </span>
                    </div>
                    <p class="mb-3"><%= h(notificacion.contenido()) %></p>
                    <div class="d-flex flex-wrap gap-2">
                        <% if (notificacion.idReceta() != null) { %>
                        <a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/mis-recetas">Ver indicaciones médicas</a>
                        <% } %>
                        <% if (notificacion.idCita() != null) { %>
                        <a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/mis-citas">Ver mis citas</a>
                        <% } %>
                        <% if (!notificacion.estaLeida()) { %>
                        <form method="post" action="${pageContext.request.contextPath}/mis-notificaciones">
                            <input type="hidden" name="idNotificacion" value="<%= notificacion.idNotificacion() %>">
                            <button class="btn btn-outline-secondary btn-sm">Marcar como leída</button>
                        </form>
                        <% } %>
                    </div>
                </div>
            </div>
        </article>
        <% } %>
    </div>
    <% } %>
</main>
</body>
</html>
