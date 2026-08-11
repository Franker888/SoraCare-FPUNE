<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.FiltroAgendaPagosDTO" %>
<%@ page import="py.fpune.soracare.dto.PagoResumenDTO" %>
<%@ page import="py.fpune.soracare.dto.ResultadoAgendaPagosDTO" %>
<%@ page import="py.fpune.soracare.mvc.model.MetodoPago" %>
<%@ page import="py.fpune.soracare.service.DisponibilidadService" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String u(String valor) {
        return URLEncoder.encode(valor == null ? "" : valor, StandardCharsets.UTF_8);
    }

    private String consultaPagina(FiltroAgendaPagosDTO filtro, int pagina) {
        return "cedula=" + u(filtro.cedula())
                + "&metodo=" + u(filtro.metodo())
                + "&pagina=" + pagina;
    }
%>
<%
    FiltroAgendaPagosDTO filtro = request.getAttribute("filtro") instanceof FiltroAgendaPagosDTO
            ? (FiltroAgendaPagosDTO) request.getAttribute("filtro")
            : FiltroAgendaPagosDTO.vacio(20);
    ResultadoAgendaPagosDTO resultado = request.getAttribute("resultado") instanceof ResultadoAgendaPagosDTO
            ? (ResultadoAgendaPagosDTO) request.getAttribute("resultado")
            : ResultadoAgendaPagosDTO.vacio(20);
    List<PagoResumenDTO> pagos = resultado.pagos();
    List<MetodoPago> metodos = request.getAttribute("metodosPago") instanceof List<?>
            ? (List<MetodoPago>) request.getAttribute("metodosPago") : List.of();
    DateTimeFormatter fechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Gestión de pagos | SoraCare-FPUNE</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { background: #f4f7f6; }
        .cabecera { background: linear-gradient(135deg, #073f32, #0c7658); color: white; }
        .card { border: 0; border-radius: 1rem; }
        .resumen-pago { min-height: 118px; }
        .resumen-icono { width: 44px; height: 44px; border-radius: 14px; display: grid; place-items: center; }
        .metodos-filtro { display: flex; flex-wrap: wrap; gap: .55rem; }
        .metodos-filtro .btn { border-radius: 999px; }
        .tabla-pagos th { white-space: nowrap; }
        .accion-pago { min-width: 170px; }
    </style>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css">
</head>
<body class="app-shell app-reception">
<nav class="navbar bg-white border-bottom">
    <div class="container py-2">
        <a class="navbar-brand fw-bold text-success" href="${pageContext.request.contextPath}/dashboard">SoraCare-FPUNE</a>
        <div class="d-flex gap-2">
            <a class="btn btn-outline-success btn-sm" href="${pageContext.request.contextPath}/agenda/citas">Agenda</a>
            <a class="btn btn-outline-success btn-sm" href="${pageContext.request.contextPath}/dashboard">Panel</a>
        </div>
    </div>
</nav>
<header class="cabecera py-4">
    <div class="container">
        <p class="small text-uppercase mb-1">Recepción</p>
        <h1 class="h2 mb-1">Pagos y comprobantes</h1>
        <p class="mb-0">Encontrá rápidamente un pago y confirmá los cobros en efectivo.</p>
    </div>
</header>
<main class="container py-4">
    <% if (request.getAttribute("mensajeExito") != null) { %>
    <div class="alert alert-success"><%= h((String) request.getAttribute("mensajeExito")) %></div>
    <% } %>
    <% if (request.getAttribute("error") != null) { %>
    <div class="alert alert-danger"><%= h((String) request.getAttribute("error")) %></div>
    <% } %>

    <section class="row g-3 mb-4" aria-label="Resumen operativo de pagos">
        <div class="col-md-4">
            <article class="card resumen-pago shadow-sm h-100"><div class="card-body d-flex align-items-center gap-3">
                <div class="resumen-icono bg-warning-subtle text-warning-emphasis"><i class="bi bi-hourglass-split"></i></div>
                <div><div class="text-secondary small">Pagos pendientes</div><div class="h3 mb-0"><%= resultado.pagosPendientes() %></div></div>
            </div></article>
        </div>
        <div class="col-md-4">
            <article class="card resumen-pago shadow-sm h-100"><div class="card-body d-flex align-items-center gap-3">
                <div class="resumen-icono bg-success-subtle text-success"><i class="bi bi-cash-stack"></i></div>
                <div><div class="text-secondary small">Efectivo por cobrar</div><div class="h3 mb-0"><%= resultado.efectivoPendiente() %></div></div>
            </div></article>
        </div>
        <div class="col-md-4">
            <article class="card resumen-pago shadow-sm h-100"><div class="card-body d-flex align-items-center gap-3">
                <div class="resumen-icono bg-primary-subtle text-primary"><i class="bi bi-phone"></i></div>
                <div><div class="text-secondary small">Digitales por completar</div><div class="h3 mb-0"><%= resultado.digitalPendiente() %></div></div>
            </div></article>
        </div>
    </section>

    <section class="card shadow-sm mb-4">
        <div class="card-body p-4">
            <div class="mb-4">
                <h2 class="h4 mb-1">Buscar operaciones</h2>
                <p class="text-secondary mb-0">Ingresá la cédula del paciente o elegí directamente un método de pago.</p>
            </div>
            <form method="get" action="${pageContext.request.contextPath}/agenda/pagos" class="row g-3 align-items-end">
                <input type="hidden" name="metodo" value="<%= h(filtro.metodo()) %>">
                <div class="col-lg-7">
                    <label class="form-label" for="cedula">Cédula del paciente</label>
                    <div class="input-group">
                        <span class="input-group-text"><i class="bi bi-search"></i></span>
                        <input class="form-control" id="cedula" name="cedula" inputmode="numeric"
                               pattern="[0-9]*" maxlength="20" placeholder="Ej.: 1234567"
                               value="<%= h(filtro.cedula()) %>">
                    </div>
                    <div class="form-text">Ingresá el número completo para encontrar exactamente al paciente.</div>
                </div>
                <div class="col-lg-5 d-flex flex-wrap gap-2">
                    <button class="btn btn-primary px-4" type="submit"><i class="bi bi-search me-2"></i>Buscar paciente</button>
                    <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/agenda/pagos">Limpiar</a>
                </div>
            </form>

            <hr class="my-4">
            <div>
                <label class="form-label d-block">Separar por método de pago</label>
                <form method="get" action="${pageContext.request.contextPath}/agenda/pagos"
                      class="metodos-filtro" role="group" aria-label="Filtrar por método de pago">
                    <input type="hidden" name="cedula" value="<%= h(filtro.cedula()) %>">
                    <button class="btn <%= filtro.metodo().isBlank() ? "btn-primary" : "btn-outline-primary" %>"
                            type="submit" name="metodo" value="">Todos</button>
                    <% for (MetodoPago metodo : metodos) {
                        String codigoMetodo = metodo.getCodigo();
                    %>
                    <button class="btn <%= codigoMetodo.equals(filtro.metodo()) ? "btn-primary" : "btn-outline-primary" %>"
                            type="submit" name="metodo" value="<%= h(codigoMetodo) %>"><%= h(metodo.getNombre()) %></button>
                    <% } %>
                </form>
                <div class="form-text mt-2">El filtro se aplica inmediatamente al pulsar un método.</div>
            </div>
        </div>
    </section>

    <section class="card shadow-sm">
        <div class="card-body p-4">
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                <div>
                    <h2 class="h4 mb-1">Resultados</h2>
                    <p class="text-secondary mb-0">
                        <%= resultado.totalResultados() %> <%= resultado.totalResultados() == 1 ? "pago encontrado" : "pagos encontrados" %>.
                        Los pendientes aparecen primero.
                    </p>
                </div>
                <% if (resultado.totalPaginas() > 0) { %>
                <span class="badge text-bg-primary">Página <%= resultado.paginaActual() %> de <%= resultado.totalPaginas() %></span>
                <% } %>
            </div>

            <% if (pagos.isEmpty()) { %>
            <div class="alert alert-light border mb-0">
                No encontramos pagos para esa cédula y método. Revisá el número o seleccioná otra opción.
            </div>
            <% } else { %>
            <div class="table-responsive">
                <table class="table align-middle tabla-pagos">
                    <thead><tr><th>Cita</th><th>Paciente</th><th>Método</th><th>Monto</th><th>Estado</th><th>Comprobante</th><th>Acción</th></tr></thead>
                    <tbody>
                    <% for (PagoResumenDTO pago : pagos) { %>
                    <tr>
                        <td><strong><%= pago.fechaCita().atZoneSameInstant(DisponibilidadService.ZONA_HORARIA).format(fechaHora) %></strong><br><small><%= h(pago.nombreEspecialidad()) %></small></td>
                        <td><%= h(pago.nombrePaciente()) %><br><small class="text-secondary">C.I. <%= h(pago.cedulaPaciente()) %> · <%= h(pago.nombreMedico()) %></small></td>
                        <td><span class="badge rounded-pill text-bg-light border"><%= h(pago.nombreMetodo()) %></span></td>
                        <td>Gs. <%= pago.monto().setScale(0).toPlainString() %></td>
                        <td><span class="badge <%= pago.estaConfirmado() ? "text-bg-success" : (pago.estaPendiente() ? "text-bg-warning" : "text-bg-secondary") %>"><%= h(pago.nombreEstado()) %></span></td>
                        <td>
                            <% if (pago.idComprobante() != null) { %>
                            <a target="_blank" href="${pageContext.request.contextPath}/comprobantes/pago?id=<%= pago.idComprobante() %>"><%= h(pago.numeroComprobante()) %></a><br><small class="text-secondary">Correo simulado</small>
                            <% } else { %><span class="text-secondary">Pendiente</span><% } %>
                        </td>
                        <td class="accion-pago">
                            <% if (pago.estaPendiente() && pago.esEfectivo()) { %>
                            <form method="post" action="${pageContext.request.contextPath}/agenda/pagos">
                                <input type="hidden" name="accion" value="confirmarEfectivo">
                                <input type="hidden" name="idPago" value="<%= pago.idPago() %>">
                                <input type="hidden" name="cedula" value="<%= h(filtro.cedula()) %>">
                                <input type="hidden" name="metodo" value="<%= h(filtro.metodo()) %>">
                                <input type="hidden" name="pagina" value="<%= resultado.paginaActual() %>">
                                <button class="btn btn-sm btn-success">Confirmar cobro</button>
                            </form>
                            <% } else if (pago.estaPendiente()) { %>
                            <small class="text-secondary">Pendiente de pago del paciente</small>
                            <% } else { %><span class="text-secondary">—</span><% } %>
                        </td>
                    </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>

            <% if (resultado.totalPaginas() > 1) { %>
            <nav class="d-flex flex-wrap justify-content-between align-items-center gap-3 pt-3 border-top" aria-label="Paginación de pagos">
                <small class="text-secondary">Mostrando hasta <%= resultado.tamanoPagina() %> resultados por página.</small>
                <div class="btn-group" role="group">
                    <% if (resultado.tienePaginaAnterior()) { %>
                    <a class="btn btn-outline-primary" href="${pageContext.request.contextPath}/agenda/pagos?<%= consultaPagina(filtro, resultado.paginaActual() - 1) %>">Anterior</a>
                    <% } else { %><span class="btn btn-outline-secondary disabled">Anterior</span><% } %>
                    <span class="btn btn-light disabled"><%= resultado.paginaActual() %> / <%= resultado.totalPaginas() %></span>
                    <% if (resultado.tienePaginaSiguiente()) { %>
                    <a class="btn btn-outline-primary" href="${pageContext.request.contextPath}/agenda/pagos?<%= consultaPagina(filtro, resultado.paginaActual() + 1) %>">Siguiente</a>
                    <% } else { %><span class="btn btn-outline-secondary disabled">Siguiente</span><% } %>
                </div>
            </nav>
            <% } %>
            <% } %>
        </div>
    </section>
</main>
</body>
</html>
