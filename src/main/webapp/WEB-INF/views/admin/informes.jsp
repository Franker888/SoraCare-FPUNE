<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.FiltroInformeDTO" %>
<%@ page import="py.fpune.soracare.dto.InformeAdministrativoDTO" %>
<%@ page import="py.fpune.soracare.mvc.model.Especialidad" %>
<%@ page import="py.fpune.soracare.mvc.model.Medico" %>
<%@ page import="py.fpune.soracare.mvc.model.MedicoEspecialidad" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.LinkedHashMap" %>

<%!
    private String h(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private int nivelCalor(long cantidad, long maximo) {
        if (cantidad <= 0 || maximo <= 0) return 0;
        double proporcion = (double) cantidad / maximo;
        if (proporcion <= 0.25) return 1;
        if (proporcion <= 0.50) return 2;
        if (proporcion <= 0.75) return 3;
        return 4;
    }

    private String nombreDia(int dia) {
        String[] dias = {"", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
        return dia >= 1 && dia <= 7 ? dias[dia] : "";
    }
%>

<%
    FiltroInformeDTO filtro = (FiltroInformeDTO) request.getAttribute("filtro");
    InformeAdministrativoDTO informe = (InformeAdministrativoDTO) request.getAttribute("informe");
    if (informe == null) informe = InformeAdministrativoDTO.vacio();
    List<Medico> medicos = request.getAttribute("medicos") instanceof List<?>
            ? (List<Medico>) request.getAttribute("medicos") : List.of();
    List<Especialidad> especialidades = request.getAttribute("especialidades") instanceof List<?>
            ? (List<Especialidad>) request.getAttribute("especialidades") : List.of();
    Map<Long, List<MedicoEspecialidad>> especialidadesPorMedico =
            request.getAttribute("especialidadesPorMedico") instanceof Map<?, ?>
                    ? (Map<Long, List<MedicoEspecialidad>>) request.getAttribute("especialidadesPorMedico")
                    : Map.of();
    String error = (String) request.getAttribute("error");

    NumberFormat numero = NumberFormat.getIntegerInstance(new Locale("es", "PY"));
    DateTimeFormatter fechaCorta = DateTimeFormatter.ofPattern("dd/MM");
    DateTimeFormatter fechaCompleta = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    Double variacion = informe.variacionCitasPorcentual();
    String claseVariacion = variacion == null || variacion == 0
            ? "variacion-neutra" : (variacion > 0 ? "variacion-positiva" : "variacion-negativa");
    String iconoVariacion = variacion == null || variacion == 0
            ? "bi-dash-lg" : (variacion > 0 ? "bi-arrow-up-right" : "bi-arrow-down-right");

    long maximoSerie = Math.max(1, informe.maximoSerie());
    int cantidadPuntos = informe.citasPorFecha().size();
    StringBuilder puntosLinea = new StringBuilder();
    StringBuilder puntosArea = new StringBuilder("42,190 ");
    for (int i = 0; i < cantidadPuntos; i++) {
        InformeAdministrativoDTO.PuntoFechaDTO punto = informe.citasPorFecha().get(i);
        double x = cantidadPuntos <= 1 ? 390 : 42D + (696D * i / (cantidadPuntos - 1D));
        double y = 190D - (150D * punto.cantidad() / maximoSerie);
        String coordenada = String.format(Locale.US, "%.1f,%.1f", x, y);
        if (puntosLinea.length() > 0) puntosLinea.append(' ');
        puntosLinea.append(coordenada);
        puntosArea.append(coordenada).append(' ');
    }
    puntosArea.append("738,190");

    Map<String, Long> calor = new LinkedHashMap<>();
    for (InformeAdministrativoDTO.CeldaDemandaDTO celda : informe.mapaCalor()) {
        calor.put(celda.diaSemana() + "-" + celda.hora(), celda.cantidad());
    }
    int horaMinima = 7;
    int horaMaxima = 20;
    for (InformeAdministrativoDTO.CeldaDemandaDTO celda : informe.mapaCalor()) {
        horaMinima = Math.min(horaMinima, celda.hora());
        horaMaxima = Math.max(horaMaxima, celda.hora());
    }
    List<Integer> horas = new java.util.ArrayList<>();
    for (int hora = horaMinima; hora <= horaMaxima; hora++) horas.add(hora);
%>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>Informes y demanda | SoraCare-FPUNE</title>
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/libs/bootstrap-icons/bootstrap-icons.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260815b">
    <style>
        body { background: #f3f7fc; }
        .barra-superior { background: rgba(255, 255, 255, .96); border-bottom: 1px solid #e2e8f0; }
        .marca img { width: 44px; height: 44px; object-fit: contain; }
        .hero-informes { position: relative; overflow: hidden; border: 0; border-radius: 1.35rem;
            color: #fff; background: linear-gradient(128deg, #173d9c 0%, #2563eb 54%, #0891b2 112%); }
        .hero-informes::after { position: absolute; width: 300px; height: 300px; right: -60px; top: -150px;
            content: ""; border-radius: 50%; background: rgba(255,255,255,.12); }
        .hero-icono { width: 76px; height: 76px; display: grid; place-items: center; border-radius: 1.25rem;
            background: rgba(255,255,255,.15); border: 1px solid rgba(255,255,255,.2); font-size: 2rem; }
        .panel { border: 1px solid #dce6f2; border-radius: 1.1rem; }
        .filtros { margin-top: -1.75rem; position: relative; z-index: 2; }
        .metric-card { height: 100%; border: 1px solid #e0e8f2; border-radius: 1rem; background: #fff; }
        .metric-icon { width: 44px; height: 44px; display: grid; place-items: center; border-radius: .85rem;
            color: #1d4ed8; background: #eaf1ff; font-size: 1.15rem; }
        .metric-value { color: #0f172a; font-size: 1.75rem; font-weight: 750; line-height: 1.1; }
        .variacion { display: inline-flex; align-items: center; gap: .25rem; padding: .25rem .5rem;
            border-radius: 999px; font-size: .78rem; font-weight: 700; }
        .variacion-positiva { color: #047857; background: #d1fae5; }
        .variacion-negativa { color: #b91c1c; background: #fee2e2; }
        .variacion-neutra { color: #475569; background: #e2e8f0; }
        .chart-shell { min-height: 285px; border-radius: 1rem;
            background: linear-gradient(180deg, #f8fbff, #fff); }
        .chart-shell svg { width: 100%; min-height: 220px; overflow: visible; }
        .grid-line { stroke: #dbe5f1; stroke-width: 1; stroke-dasharray: 4 5; }
        .linea-demanda { fill: none; stroke: #2563eb; stroke-width: 4; stroke-linecap: round; stroke-linejoin: round; }
        .area-demanda { fill: url(#gradienteDemanda); }
        .punto-demanda { fill: #fff; stroke: #2563eb; stroke-width: 3; }
        .barra-fondo { height: 10px; overflow: hidden; border-radius: 999px; background: #e9eff7; }
        .barra-valor { height: 100%; border-radius: inherit; background: linear-gradient(90deg, #2563eb, #0ea5a8); }
        .ranking-numero { width: 34px; height: 34px; display: grid; place-items: center; border-radius: .7rem;
            color: #1d4ed8; background: #eaf1ff; font-weight: 800; }
        .estado-pill { padding: .55rem .75rem; border: 1px solid #dce6f2; border-radius: .8rem; background: #f8fafc; }
        .heatmap-wrap { overflow-x: auto; }
        .heatmap { display: grid; grid-template-columns: 64px repeat(var(--heatmap-columnas), minmax(42px, 1fr)); gap: 5px; }
        .heat-label { display: grid; place-items: center; min-height: 38px; color: #64748b; font-size: .78rem; font-weight: 700; }
        .heat-cell { display: grid; place-items: center; min-height: 38px; border-radius: .55rem;
            color: #475569; background: #edf2f7; font-size: .76rem; font-weight: 700; }
        .heat-1 { color: #0f766e; background: #ccfbf1; }
        .heat-2 { color: #0f766e; background: #99f6e4; }
        .heat-3 { color: #fff; background: #2dd4bf; }
        .heat-4 { color: #fff; background: #0f766e; }
        .leyenda-calor span { width: 18px; height: 18px; display: inline-block; border-radius: .35rem; }
        .privacidad { border-left: 4px solid #0ea5a8; background: #f0fdfa; }
        @media (max-width: 767.98px) {
            .filtros { margin-top: 1rem; }
            .hero-informes .display-6 { font-size: 2rem; }
        }
    </style>
</head>
<body class="app-shell app-admin">

<nav class="navbar barra-superior sticky-top">
    <div class="container py-2">
        <a class="navbar-brand marca d-flex align-items-center gap-2 fw-bold text-primary"
           href="${pageContext.request.contextPath}/admin/inicio">
            <img src="${pageContext.request.contextPath}/assets/home/img/logo/SoraCare.png" alt="Logo SoraCare">
            <span>SoraCare-FPUNE</span>
        </a>
        <div class="d-flex gap-2">
            <a class="btn btn-outline-primary btn-sm" href="${pageContext.request.contextPath}/admin/inicio">
                <i class="bi bi-grid me-1"></i> Administración
            </a>
            <form method="post" action="${pageContext.request.contextPath}/logout" class="m-0"><input type="hidden" name="csrfToken" value="${requestScope.csrfToken}">
                <button class="btn btn-outline-secondary btn-sm">Cerrar sesión</button>
            </form>
        </div>
    </div>
</nav>

<main class="container py-4 py-lg-5">
    <section class="card hero-informes shadow-sm mb-0">
        <div class="card-body p-4 p-lg-5 position-relative z-1">
            <div class="row align-items-center g-4">
                <div class="col-lg-8">
                    <p class="mb-2 opacity-75 fw-semibold">Etapa 8 · Inteligencia administrativa</p>
                    <h1 class="display-6 fw-bold mb-3">Informes y análisis de demanda</h1>
                    <p class="lead mb-0 opacity-90">Descubrí cuándo, dónde y con qué profesionales se concentra la solicitud de atención.</p>
                </div>
                <div class="col-lg-4 d-flex justify-content-lg-end">
                    <div class="hero-icono"><i class="bi bi-graph-up-arrow"></i></div>
                </div>
            </div>
        </div>
    </section>

    <section class="card panel filtros shadow-sm mb-4 mx-lg-4">
        <div class="card-body p-4">
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                <div>
                    <h2 class="h5 mb-1">Período y alcance</h2>
                    <p class="text-secondary mb-0">Combiná fechas, especialidad y médico.</p>
                </div>
                <span class="badge rounded-pill text-bg-light border px-3 py-2">
                    <i class="bi bi-calendar3 me-1"></i>
                    <%= filtro.cantidadDias() %> día<%= filtro.cantidadDias() == 1 ? "" : "s" %>
                </span>
            </div>
            <form method="get" action="${pageContext.request.contextPath}/admin/informes" class="row g-3 align-items-end">
                <div class="col-sm-6 col-xl-2">
                    <label for="desde" class="form-label">Desde</label>
                    <input type="date" id="desde" name="desde" class="form-control" required value="<%= filtro.fechaDesde() %>">
                </div>
                <div class="col-sm-6 col-xl-2">
                    <label for="hasta" class="form-label">Hasta</label>
                    <input type="date" id="hasta" name="hasta" class="form-control" required value="<%= filtro.fechaHasta() %>">
                </div>
                <div class="col-md-5 col-xl-3">
                    <label for="idEspecialidad" class="form-label">Especialidad</label>
                    <select id="idEspecialidad" name="idEspecialidad" class="form-select">
                        <option value="">Todas las especialidades</option>
                        <% for (Especialidad especialidad : especialidades) { %>
                        <option value="<%= especialidad.getIdEspecialidad() %>"
                                <%= especialidad.getIdEspecialidad().equals(filtro.idEspecialidad()) ? "selected" : "" %>>
                            <%= h(especialidad.getNombre()) %>
                        </option>
                        <% } %>
                    </select>
                </div>
                <div class="col-md-5 col-xl-3">
                    <label for="idMedico" class="form-label">Médico</label>
                    <select id="idMedico" name="idMedico" class="form-select">
                        <option value="">Todos los médicos</option>
                        <% for (Medico medico : medicos) {
                            StringBuilder idsEspecialidades = new StringBuilder();
                            for (MedicoEspecialidad relacion : especialidadesPorMedico.getOrDefault(medico.getIdMedico(), List.of())) {
                                if (idsEspecialidades.length() > 0) idsEspecialidades.append(',');
                                idsEspecialidades.append(relacion.getIdEspecialidad());
                            }
                        %>
                        <option value="<%= medico.getIdMedico() %>"
                                data-especialidades="<%= idsEspecialidades %>"
                                <%= medico.getIdMedico().equals(filtro.idMedico()) ? "selected" : "" %>>
                            <%= h(medico.getNombres() + " " + medico.getApellidos()) %>
                        </option>
                        <% } %>
                    </select>
                </div>
                <div class="col-md-2 col-xl-2 d-grid">
                    <button class="btn btn-primary"><i class="bi bi-funnel me-1"></i> Aplicar</button>
                </div>
            </form>
        </div>
    </section>

    <% if (error != null && !error.isBlank()) { %>
    <div class="alert alert-danger shadow-sm"><i class="bi bi-exclamation-triangle me-2"></i><%= h(error) %></div>
    <% } %>

    <section class="row g-3 mb-4">
        <div class="col-6 col-xl-3">
            <article class="metric-card shadow-sm p-3 p-lg-4">
                <div class="d-flex justify-content-between gap-2 mb-3">
                    <div class="metric-icon"><i class="bi bi-calendar2-check"></i></div>
                    <span class="variacion <%= claseVariacion %>">
                        <i class="bi <%= iconoVariacion %>"></i>
                        <% if (variacion == null) { %>Nueva demanda<% } else { %><%= String.format(Locale.US, "%.1f%%", Math.abs(variacion)) %><% } %>
                    </span>
                </div>
                <div class="metric-value"><%= numero.format(informe.totalCitas()) %></div>
                <div class="text-secondary mt-1">Citas del período</div>
                <small class="text-secondary"><%= numero.format(informe.citasPeriodoAnterior()) %> en el período anterior</small>
            </article>
        </div>
        <div class="col-6 col-xl-3">
            <article class="metric-card shadow-sm p-3 p-lg-4">
                <div class="metric-icon mb-3"><i class="bi bi-search-heart"></i></div>
                <div class="metric-value"><%= numero.format(informe.busquedasTurnos()) %></div>
                <div class="text-secondary mt-1">Búsquedas de turnos</div>
                <small class="text-secondary">Se acumulan desde la etapa 8</small>
            </article>
        </div>
        <div class="col-6 col-xl-3">
            <article class="metric-card shadow-sm p-3 p-lg-4">
                <div class="metric-icon mb-3"><i class="bi bi-cash-stack"></i></div>
                <div class="metric-value fs-3">Gs. <%= numero.format(informe.ingresosConfirmados()) %></div>
                <div class="text-secondary mt-1">Pagos confirmados</div>
                <small class="text-secondary">No incluye pendientes o reembolsados</small>
            </article>
        </div>
        <div class="col-6 col-xl-3">
            <article class="metric-card shadow-sm p-3 p-lg-4">
                <div class="metric-icon mb-3"><i class="bi bi-person-plus"></i></div>
                <div class="metric-value"><%= numero.format(informe.pacientesRegistrados()) %></div>
                <div class="text-secondary mt-1">Pacientes nuevos</div>
                <small class="text-secondary"><%= numero.format(informe.citasReprogramadas()) %> reprogramaciones</small>
            </article>
        </div>
    </section>

    <section class="row g-4 mb-4">
        <div class="col-12 col-xl-8">
            <article class="card panel shadow-sm h-100">
                <div class="card-body p-4">
                    <div class="d-flex flex-wrap justify-content-between gap-3 mb-3">
                        <div>
                            <h2 class="h5 mb-1">Evolución diaria de citas</h2>
                            <p class="text-secondary mb-0">La curva permite detectar aumentos y descensos durante el período.</p>
                        </div>
                        <span class="badge text-bg-primary align-self-start px-3 py-2">Demanda reservada</span>
                    </div>
                    <div class="chart-shell p-2 p-md-3">
                        <svg viewBox="0 0 780 230" role="img" aria-label="Evolución de citas por día">
                            <defs>
                                <linearGradient id="gradienteDemanda" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="0%" stop-color="#2563eb" stop-opacity=".26"/>
                                    <stop offset="100%" stop-color="#2563eb" stop-opacity=".02"/>
                                </linearGradient>
                            </defs>
                            <line class="grid-line" x1="42" x2="738" y1="40" y2="40"/>
                            <line class="grid-line" x1="42" x2="738" y1="90" y2="90"/>
                            <line class="grid-line" x1="42" x2="738" y1="140" y2="140"/>
                            <line class="grid-line" x1="42" x2="738" y1="190" y2="190"/>
                            <% if (!informe.citasPorFecha().isEmpty()) { %>
                            <polygon class="area-demanda" points="<%= puntosArea %>"/>
                            <polyline class="linea-demanda" points="<%= puntosLinea %>"/>
                            <% for (int i = 0; i < cantidadPuntos; i++) {
                                InformeAdministrativoDTO.PuntoFechaDTO punto = informe.citasPorFecha().get(i);
                                double x = cantidadPuntos <= 1 ? 390 : 42D + (696D * i / (cantidadPuntos - 1D));
                                double y = 190D - (150D * punto.cantidad() / maximoSerie);
                            %>
                            <circle class="punto-demanda" cx="<%= String.format(Locale.US, "%.1f", x) %>"
                                    cy="<%= String.format(Locale.US, "%.1f", y) %>" r="4">
                                <title><%= fechaCompleta.format(punto.fecha()) %>: <%= punto.cantidad() %> citas</title>
                            </circle>
                            <% } %>
                            <% } %>
                            <text x="42" y="218" fill="#64748b" font-size="12"><%= fechaCorta.format(filtro.fechaDesde()) %></text>
                            <text x="738" y="218" text-anchor="end" fill="#64748b" font-size="12"><%= fechaCorta.format(filtro.fechaHasta()) %></text>
                        </svg>
                    </div>
                </div>
            </article>
        </div>

        <div class="col-12 col-xl-4">
            <article class="card panel shadow-sm h-100">
                <div class="card-body p-4">
                    <h2 class="h5 mb-1">Estado de las citas</h2>
                    <p class="text-secondary mb-4">Situación actual dentro del período seleccionado.</p>
                    <div class="row g-2 mb-4">
                        <div class="col-6"><div class="estado-pill"><strong class="d-block text-primary fs-5"><%= informe.citasConfirmadas() %></strong><small>Confirmadas</small></div></div>
                        <div class="col-6"><div class="estado-pill"><strong class="d-block text-success fs-5"><%= informe.citasAtendidas() %></strong><small>Atendidas</small></div></div>
                        <div class="col-6"><div class="estado-pill"><strong class="d-block text-danger fs-5"><%= informe.citasCanceladas() %></strong><small>Canceladas</small></div></div>
                        <div class="col-6"><div class="estado-pill"><strong class="d-block text-info fs-5"><%= String.format(Locale.US, "%.1f%%", informe.tasaAtencion()) %></strong><small>Tasa atendida</small></div></div>
                    </div>
                    <div class="vstack gap-3">
                        <% for (InformeAdministrativoDTO.DistribucionDTO estado : informe.citasPorEstado()) { %>
                        <div class="d-flex justify-content-between gap-3 border-bottom pb-2">
                            <span class="text-secondary"><%= h(estado.etiqueta()) %></span>
                            <strong><%= numero.format(estado.cantidad()) %></strong>
                        </div>
                        <% } %>
                        <% if (informe.citasPorEstado().isEmpty()) { %>
                        <p class="text-secondary text-center py-3 mb-0">No hay citas en este período.</p>
                        <% } %>
                    </div>
                </div>
            </article>
        </div>
    </section>

    <section class="row g-4 mb-4">
        <div class="col-12 col-lg-6">
            <article class="card panel shadow-sm h-100">
                <div class="card-body p-4">
                    <div class="d-flex justify-content-between align-items-start gap-3 mb-4">
                        <div><h2 class="h5 mb-1">Especialidades más solicitadas</h2><p class="text-secondary mb-0">Comparación por citas registradas.</p></div>
                        <div class="metric-icon"><i class="bi bi-heart-pulse"></i></div>
                    </div>
                    <div class="vstack gap-3">
                        <% for (InformeAdministrativoDTO.DistribucionDTO item : informe.citasPorEspecialidad()) {
                            double ancho = informe.maximoEspecialidades() == 0 ? 0
                                    : (double) item.cantidad() * 100 / informe.maximoEspecialidades();
                        %>
                        <div>
                            <div class="d-flex justify-content-between gap-2 mb-1">
                                <span class="fw-semibold"><%= h(item.etiqueta()) %></span>
                                <span class="text-secondary"><%= item.cantidad() %> citas</span>
                            </div>
                            <div class="barra-fondo"><div class="barra-valor" style="width:<%= String.format(Locale.US, "%.1f", ancho) %>%"></div></div>
                        </div>
                        <% } %>
                        <% if (informe.citasPorEspecialidad().isEmpty()) { %>
                        <p class="text-secondary text-center py-4 mb-0">Todavía no hay demanda para comparar.</p>
                        <% } %>
                    </div>
                </div>
            </article>
        </div>

        <div class="col-12 col-lg-6">
            <article class="card panel shadow-sm h-100">
                <div class="card-body p-4">
                    <div class="d-flex justify-content-between align-items-start gap-3 mb-4">
                        <div><h2 class="h5 mb-1">Médicos con mayor demanda</h2><p class="text-secondary mb-0">Ranking según citas del período.</p></div>
                        <div class="metric-icon"><i class="bi bi-person-badge"></i></div>
                    </div>
                    <div class="vstack gap-3">
                        <% int posicion = 1; for (InformeAdministrativoDTO.DistribucionDTO item : informe.citasPorMedico()) { %>
                        <div class="d-flex align-items-center gap-3 border-bottom pb-3">
                            <div class="ranking-numero"><%= posicion++ %></div>
                            <div class="flex-grow-1"><strong class="d-block"><%= h(item.etiqueta()) %></strong><small class="text-secondary">Profesional solicitado</small></div>
                            <span class="badge rounded-pill text-bg-primary"><%= item.cantidad() %> citas</span>
                        </div>
                        <% } %>
                        <% if (informe.citasPorMedico().isEmpty()) { %>
                        <p class="text-secondary text-center py-4 mb-0">Todavía no hay médicos para comparar.</p>
                        <% } %>
                    </div>
                </div>
            </article>
        </div>
    </section>

    <section class="card panel shadow-sm mb-4">
        <div class="card-body p-4">
            <div class="d-flex flex-wrap justify-content-between gap-3 mb-4">
                <div>
                    <h2 class="h5 mb-1">Mapa de concentración por día y hora</h2>
                    <p class="text-secondary mb-0">Cuanto más intenso es el color, mayor fue la cantidad de citas.</p>
                </div>
                <div class="d-flex align-items-center gap-2 leyenda-calor text-secondary small">
                    <span class="heat-cell"></span> Baja
                    <span class="heat-cell heat-2"></span>
                    <span class="heat-cell heat-4"></span> Alta
                </div>
            </div>
            <div class="heatmap-wrap pb-2">
                <div class="heatmap" style="--heatmap-columnas: <%= horas.size() %>; min-width: <%= 70 + horas.size() * 47 %>px;">
                    <div></div>
                    <% for (int hora : horas) { %><div class="heat-label"><%= String.format("%02d:00", hora) %></div><% } %>
                    <% for (int dia = 1; dia <= 7; dia++) { %>
                    <div class="heat-label"><%= nombreDia(dia) %></div>
                    <% for (int hora : horas) {
                        long cantidad = calor.getOrDefault(dia + "-" + hora, 0L);
                        int nivel = nivelCalor(cantidad, informe.maximoMapaCalor());
                    %>
                    <div class="heat-cell heat-<%= nivel %>" title="<%= nombreDia(dia) %> <%= hora %>:00 · <%= cantidad %> citas">
                        <%= cantidad == 0 ? "–" : cantidad %>
                    </div>
                    <% } %>
                    <% } %>
                </div>
            </div>
        </div>
    </section>

    <aside class="alert privacidad d-flex gap-3 align-items-start shadow-sm mb-0">
        <i class="bi bi-shield-check fs-4 text-success"></i>
        <div><strong>Análisis respetuoso de la privacidad</strong><div class="text-secondary">Las búsquedas de turnos se contabilizan sin guardar nombre, cédula, dirección IP ni información clínica.</div></div>
    </aside>
</main>

<script>
    const especialidad = document.getElementById('idEspecialidad');
    const medico = document.getElementById('idMedico');
    function filtrarMedicos() {
        const seleccion = especialidad.value;
        [...medico.options].forEach((opcion, indice) => {
            if (indice === 0) return;
            const permitidas = (opcion.dataset.especialidades || '').split(',');
            const visible = !seleccion || permitidas.includes(seleccion);
            opcion.hidden = !visible;
            opcion.disabled = !visible;
        });
        if (medico.selectedOptions[0] && medico.selectedOptions[0].disabled) medico.value = '';
    }
    especialidad.addEventListener('change', filtrarMedicos);
    filtrarMedicos();
</script>
</body>
</html>
