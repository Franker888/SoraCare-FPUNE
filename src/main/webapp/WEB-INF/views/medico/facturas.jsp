<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.PagoResumenDTO" %>
<%@ page import="py.fpune.soracare.service.DisponibilidadService" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    List<PagoResumenDTO> pagos = request.getAttribute("pagosConfirmados") instanceof List<?>
            ? (List<PagoResumenDTO>) request.getAttribute("pagosConfirmados")
            : List.of();
    String csrfToken = (String) request.getAttribute("csrfToken");
    long emitidas = pagos.stream().filter(p -> p.idFacturaInterna() != null).count();
    long pendientes = pagos.size() - emitidas;
    DateTimeFormatter fechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title>Facturación interna | SoraCare-FPUNE</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/libs/bootstrap-icons/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260831a">
    <style>
        .billing-hero { background: linear-gradient(135deg,#173f98,#168da6); color:#fff; border-radius:1.4rem; padding:2rem; }
        .billing-stat { border:0; border-radius:1rem; box-shadow:0 12px 32px rgba(25,55,95,.08); }
        .billing-table-card { border:0; border-radius:1.2rem; box-shadow:0 12px 36px rgba(25,55,95,.08); overflow:hidden; }
        .billing-warning { border-left:.3rem solid #e0a800; }
        .billing-number { font-family:ui-monospace,SFMono-Regular,Menlo,monospace; }
    </style>
</head>
<body class="app-shell">
<%@ include file="_navbar.jspf" %>
<main class="container py-4 py-lg-5">
    <section class="billing-hero mb-4">
        <div class="row align-items-center g-3">
            <div class="col">
                <p class="text-white-50 fw-semibold mb-2">Gestión comercial del profesional</p>
                <h1 class="h2 mb-2">Facturación interna</h1>
                <p class="mb-0">Emití el documento comercial de una consulta con pago confirmado y compartilo en PDF con el paciente.</p>
            </div>
            <div class="col-auto d-none d-md-block">
                <i class="bi bi-receipt-cutoff display-3" aria-hidden="true"></i>
            </div>
        </div>
    </section>

    <div class="alert alert-warning billing-warning" role="alert">
        <strong>Alcance actual:</strong> estos documentos son registros internos sin validez tributaria.
        No reemplazan una factura electrónica de e-Kuatia/SIFEN ni incluyen timbrado o CDC.
    </div>

    <% if (request.getAttribute("mensajeExito") != null) { %>
    <div class="alert alert-success"><%= h((String) request.getAttribute("mensajeExito")) %></div>
    <% } %>
    <% if (request.getAttribute("error") != null) { %>
    <div class="alert alert-danger"><%= h((String) request.getAttribute("error")) %></div>
    <% } %>

    <section class="row g-3 mb-4" aria-label="Resumen de facturación">
        <div class="col-md-4">
            <article class="card billing-stat h-100"><div class="card-body p-4">
                <div class="text-secondary small">Pagos confirmados</div>
                <div class="display-6 fw-bold"><%= pagos.size() %></div>
            </div></article>
        </div>
        <div class="col-md-4">
            <article class="card billing-stat h-100"><div class="card-body p-4">
                <div class="text-secondary small">Facturas emitidas</div>
                <div class="display-6 fw-bold text-success"><%= emitidas %></div>
            </div></article>
        </div>
        <div class="col-md-4">
            <article class="card billing-stat h-100"><div class="card-body p-4">
                <div class="text-secondary small">Pendientes de emisión</div>
                <div class="display-6 fw-bold text-primary"><%= pendientes %></div>
            </div></article>
        </div>
    </section>

    <section class="card billing-table-card">
        <div class="card-body p-0">
            <div class="p-4 border-bottom">
                <h2 class="h4 mb-1">Consultas cobradas</h2>
                <p class="text-secondary mb-0">Cada pago admite una sola factura interna para evitar duplicados.</p>
            </div>
            <% if (pagos.isEmpty()) { %>
            <div class="p-5 text-center text-secondary">
                <i class="bi bi-receipt display-5 d-block mb-3"></i>
                Todavía no hay pagos confirmados asociados a tus consultas.
            </div>
            <% } else { %>
            <div class="table-responsive">
                <table class="table align-middle mb-0">
                    <thead class="table-light">
                    <tr>
                        <th class="ps-4">Paciente y consulta</th>
                        <th>Fecha</th>
                        <th>Importe</th>
                        <th>Documento</th>
                        <th class="pe-4 text-end">Acción</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% for (PagoResumenDTO pago : pagos) { %>
                    <tr>
                        <td class="ps-4">
                            <strong><%= h(pago.nombrePaciente()) %></strong>
                            <div class="text-secondary small"><%= h(pago.nombreEspecialidad()) %> · CI <%= h(pago.cedulaPaciente()) %></div>
                        </td>
                        <td><%= pago.fechaCita().atZoneSameInstant(DisponibilidadService.ZONA_HORARIA).format(fechaHora) %></td>
                        <td>Gs. <strong><%= pago.monto().setScale(0).toPlainString() %></strong></td>
                        <td>
                            <% if (pago.idFacturaInterna() == null) { %>
                            <span class="badge text-bg-warning">Pendiente</span>
                            <% } else { %>
                            <span class="badge text-bg-success">Emitida</span>
                            <div class="billing-number small mt-1"><%= h(pago.numeroFacturaInterna()) %></div>
                            <% if ("ENVIADO".equals(pago.estadoEnvioFacturaInterna())) { %>
                            <div class="small text-success mt-1"><i class="bi bi-envelope-check"></i> Correo enviado</div>
                            <% } else if ("FALLIDO".equals(pago.estadoEnvioFacturaInterna())) { %>
                            <div class="small text-danger mt-1"><i class="bi bi-envelope-exclamation"></i> Falló el correo; el PDF sigue disponible</div>
                            <% } else if ("NO_CONFIGURADO".equals(pago.estadoEnvioFacturaInterna())) { %>
                            <div class="small text-secondary mt-1"><i class="bi bi-file-earmark-check"></i> PDF disponible; correo no configurado</div>
                            <% } else { %>
                            <div class="small text-secondary mt-1"><i class="bi bi-hourglass-split"></i> Preparando correo</div>
                            <% } %>
                            <% } %>
                        </td>
                        <td class="pe-4 text-end">
                            <% if (pago.idFacturaInterna() == null) { %>
                            <form method="post" action="${pageContext.request.contextPath}/medico/facturas" class="d-inline">
                                <input type="hidden" name="csrfToken" value="<%= h(csrfToken) %>">
                                <input type="hidden" name="idPago" value="<%= pago.idPago() %>">
                                <button class="btn btn-primary btn-sm">
                                    <i class="bi bi-file-earmark-plus me-1"></i>Emitir factura
                                </button>
                            </form>
                            <% } else { %>
                            <a class="btn btn-outline-primary btn-sm" target="_blank"
                               href="${pageContext.request.contextPath}/facturas/interna?id=<%= pago.idFacturaInterna() %>">
                                <i class="bi bi-file-earmark-pdf me-1"></i>Ver PDF
                            </a>
                            <% } %>
                        </td>
                    </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
            <% } %>
        </div>
    </section>
</main>
<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
</body>
</html>
