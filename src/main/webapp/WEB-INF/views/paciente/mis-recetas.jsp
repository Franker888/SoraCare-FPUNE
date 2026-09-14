<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.DetalleRecetaDTO" %>
<%@ page import="py.fpune.soracare.dto.RecetaMedicaDTO" %>
<%@ page import="py.fpune.soracare.service.DisponibilidadService" %>
<%@ page import="java.time.LocalDate" %>
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
    List<RecetaMedicaDTO> recetas = request.getAttribute("recetas") instanceof List<?>
            ? (List<RecetaMedicaDTO>) request.getAttribute("recetas") : List.of();
    String error = (String) request.getAttribute("error");
    DateTimeFormatter fechaHora = DateTimeFormatter.ofPattern(
            "dd/MM/yyyy HH:mm", new Locale("es", "PY"));
    LocalDate hoy = LocalDate.now(DisponibilidadService.ZONA_HORARIA);
%>

<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>Mis indicaciones médicas | SoraCare-FPUNE</title>
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap-icons/bootstrap-icons.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815">
    <style>
        body { background: #f4f7fb; color: #172033; }
        .hero-recetas { border: 0; border-radius: 1.3rem; color: #fff;
            background: linear-gradient(135deg, #173da8, #2563eb 58%, #13a6a2); }
        .receta { border: 1px solid #dce5f1; border-radius: 1.1rem; overflow: hidden; }
        .receta.activa { border-left: 5px solid #2563eb; }
        .receta.anulada { border-left: 5px solid #94a3b8; opacity: .78; }
        .medicamento { border: 1px solid #e2e8f0; border-radius: .9rem; background: #f8fafc; }
        .icono-receta { width: 52px; height: 52px; display: grid; place-items: center;
            border-radius: 15px; background: #eaf1ff; color: #2563eb; font-size: 1.5rem; }
        .texto-largo { white-space: pre-line; }
    </style>
</head>
<body class="app-shell app-patient">
<nav class="navbar bg-white border-bottom">
    <div class="container py-2">
        <a class="navbar-brand fw-bold text-primary" href="${pageContext.request.contextPath}/paciente/inicio">SoraCare-FPUNE</a>
        <div class="d-flex gap-2">
            <a class="btn btn-outline-primary btn-sm" href="${pageContext.request.contextPath}/paciente/inicio">Panel</a>
            <form method="post" action="${pageContext.request.contextPath}/logout" class="m-0"><input type="hidden" name="csrfToken" value="${requestScope.csrfToken}">
                <button class="btn btn-outline-secondary btn-sm">Cerrar sesión</button>
            </form>
        </div>
    </div>
</nav>

<main class="container py-4 py-lg-5">
    <section class="card hero-recetas shadow-sm mb-4">
        <div class="card-body p-4 p-lg-5 d-flex flex-column flex-lg-row justify-content-between gap-4">
            <div>
                <p class="mb-2 opacity-75">Portal del paciente · Tratamientos</p>
                <h1 class="display-6 fw-bold mb-2">Mis indicaciones médicas</h1>
                <p class="mb-0">Consultá la medicación y los cuidados indicados por tus profesionales.</p>
            </div>
            <div class="align-self-lg-center">
                <span class="badge bg-white text-primary fs-6 px-3 py-2">
                    <%= recetas.size() == 1
                            ? "1 indicación"
                            : recetas.size() + " indicaciones" %>
                </span>
            </div>
        </div>
    </section>

    <% if (error != null && !error.isBlank()) { %>
    <div class="alert alert-danger"><%= h(error) %></div>
    <% } %>

    <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
            <h2 class="h4 mb-1">Historial de indicaciones</h2>
            <p class="text-secondary mb-0">Las indicaciones médicas más recientes aparecen primero.</p>
        </div>
        <a class="btn btn-outline-primary" href="${pageContext.request.contextPath}/mis-citas">Ir a mis citas</a>
    </div>

    <% if (recetas.isEmpty()) { %>
    <section class="card receta shadow-sm">
        <div class="card-body text-center py-5 px-3">
            <div class="icono-receta mx-auto mb-3"><i class="bi bi-prescription2"></i></div>
            <h2 class="h5">Todavía no tenés indicaciones médicas</h2>
            <p class="text-secondary mb-0">Cuando un médico registre una durante tu atención, aparecerá aquí.</p>
        </div>
    </section>
    <% } else { %>
    <div class="d-grid gap-4">
        <% for (RecetaMedicaDTO receta : recetas) {
            boolean vencidaPorFecha = receta.fechaVencimiento() != null
                    && receta.fechaVencimiento().isBefore(hoy);
            boolean disponible = receta.estaEmitida() && !vencidaPorFecha;
        %>
        <article class="card receta shadow-sm <%= receta.estaEmitida() ? "activa" : "anulada" %>">
            <div class="card-body p-4">
                <div class="d-flex flex-column flex-lg-row justify-content-between gap-3 mb-4">
                    <div class="d-flex gap-3">
                        <div class="icono-receta"><i class="bi bi-prescription2"></i></div>
                        <div>
                            <p class="text-secondary small mb-1">Código de la indicación</p>
                            <h2 class="h5 mb-1"><%= h(receta.codigo()) %></h2>
                            <div class="text-secondary">
                                <%= h(receta.nombreMedico()) %> · <%= h(receta.especialidad()) %>
                            </div>
                        </div>
                    </div>
                    <div class="text-lg-end">
                        <span class="badge <%= disponible ? "text-bg-success" : "text-bg-secondary" %>">
                            <%= receta.estaEmitida() ? (vencidaPorFecha ? "VENCIDA" : "EMITIDA") : h(receta.estado()) %>
                        </span>
                        <div class="text-secondary small mt-2">
                            Emitida: <%= receta.fechaEmision().atZoneSameInstant(
                                    DisponibilidadService.ZONA_HORARIA).format(fechaHora) %>
                            <% if (receta.fechaVencimiento() != null) { %>
                            <br>Válida hasta: <%= receta.fechaVencimiento() %>
                            <% } %>
                        </div>
                        <% if (!"NO_SOLICITADO".equals(receta.estadoEnvioIndicacionesEmail())) { %>
                        <span class="badge mt-2 <%= "ENVIADO".equals(receta.estadoEnvioIndicacionesEmail())
                                ? "text-bg-primary"
                                : ("FALLIDO".equals(receta.estadoEnvioIndicacionesEmail())
                                ? "text-bg-danger" : "text-bg-warning") %>">
                            <i class="bi bi-envelope-check me-1"></i>
                            <%= h(receta.textoEstadoEnvioIndicaciones()) %>
                        </span>
                        <% } %>
                    </div>
                </div>

                <div class="d-grid gap-2 mb-3">
                    <% for (DetalleRecetaDTO detalle : receta.detalles()) { %>
                    <div class="medicamento p-3">
                        <div class="d-flex flex-column flex-md-row justify-content-between gap-2">
                            <div>
                                <strong><%= h(detalle.nombreComercial()) %>
                                    <%= h(detalle.concentracion()) %></strong>
                                <div class="text-secondary small">
                                    <%= h(detalle.principioActivo()) %> ·
                                    <%= h(detalle.presentacion()) %> ·
                                    <%= h(detalle.viaAdministracion().getNombreVisible()) %>
                                </div>
                            </div>
                            <div class="text-md-end">
                                <strong><%= h(detalle.dosis()) %></strong>
                                <div><%= h(detalle.frecuencia()) %></div>
                            </div>
                        </div>
                        <div class="small mt-2">
                            Cantidad: <%= detalle.cantidad().stripTrailingZeros().toPlainString() %>
                            <%= h(detalle.unidad()) %>
                            <% if (detalle.duracionDias() != null) { %>
                            · Durante <%= detalle.duracionDias() %> días
                            <% } %>
                            <% if (detalle.instrucciones() != null) { %>
                            · <%= h(detalle.instrucciones()) %>
                            <% } %>
                        </div>
                    </div>
                    <% } %>
                </div>

                <% if (receta.indicacionesGenerales() != null) { %>
                <div class="alert alert-primary texto-largo">
                    <strong>Indicaciones generales:</strong> <%= h(receta.indicacionesGenerales()) %>
                </div>
                <% } %>
                <% if (receta.motivoAnulacion() != null) { %>
                <div class="alert alert-secondary">
                    <strong>Indicación anulada:</strong> <%= h(receta.motivoAnulacion()) %>
                </div>
                <% } %>

                <div class="d-flex flex-wrap justify-content-end gap-2 border-top pt-3">
                    <a class="btn btn-outline-primary"
                       href="${pageContext.request.contextPath}/mis-recetas/pdf?id=<%= receta.idReceta() %>">
                        Ver PDF
                    </a>
                    <a class="btn btn-primary"
                       href="${pageContext.request.contextPath}/mis-recetas/pdf?id=<%= receta.idReceta() %>&descargar=1">
                        <i class="bi bi-download me-1"></i> Descargar
                    </a>
                </div>
            </div>
        </article>
        <% } %>
    </div>
    <% } %>
</main>
</body>
</html>
