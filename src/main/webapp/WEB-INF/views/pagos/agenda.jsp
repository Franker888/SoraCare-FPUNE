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
    List<PagoResumenDTO> pagos = request.getAttribute("pagos") instanceof List<?>
            ? (List<PagoResumenDTO>) request.getAttribute("pagos") : List.of();
    DateTimeFormatter fechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Gestión de pagos | SoraCare-FPUNE</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>body{background:#f4f7f6}.cabecera{background:linear-gradient(135deg,#073f32,#0c7658);color:white}.card{border:0;border-radius:1rem}</style>
</head>
<body>
<nav class="navbar bg-white border-bottom"><div class="container py-2">
    <a class="navbar-brand fw-bold text-success" href="${pageContext.request.contextPath}/dashboard">SoraCare-FPUNE</a>
    <div class="d-flex gap-2"><a class="btn btn-outline-success btn-sm" href="${pageContext.request.contextPath}/agenda/citas">Agenda</a><a class="btn btn-outline-success btn-sm" href="${pageContext.request.contextPath}/dashboard">Panel</a></div>
</div></nav>
<header class="cabecera py-4"><div class="container"><p class="small text-uppercase mb-1">Etapa 5</p><h1 class="h2 mb-1">Pagos y comprobantes</h1><p class="mb-0">Confirmá cobros en efectivo y consultá las operaciones simuladas.</p></div></header>
<main class="container py-4">
    <% if (request.getAttribute("mensajeExito") != null) { %><div class="alert alert-success"><%= h((String) request.getAttribute("mensajeExito")) %></div><% } %>
    <% if (request.getAttribute("error") != null) { %><div class="alert alert-danger"><%= h((String) request.getAttribute("error")) %></div><% } %>
    <section class="card shadow-sm"><div class="card-body p-4">
        <div class="d-flex justify-content-between align-items-center mb-3"><div><h2 class="h4 mb-1">Operaciones recientes</h2><p class="text-secondary mb-0">Los pagos pendientes aparecen primero.</p></div><span class="badge text-bg-success"><%= pagos.size() %></span></div>
        <% if (pagos.isEmpty()) { %><div class="alert alert-light border mb-0">Todavía no hay pagos registrados.</div><% } else { %>
        <div class="table-responsive"><table class="table align-middle"><thead><tr><th>Cita</th><th>Paciente</th><th>Método</th><th>Monto</th><th>Estado</th><th>Comprobante</th><th>Acción</th></tr></thead><tbody>
        <% for (PagoResumenDTO pago : pagos) { %><tr>
            <td><strong><%= pago.fechaCita().atZoneSameInstant(DisponibilidadService.ZONA_HORARIA).format(fechaHora) %></strong><br><small><%= h(pago.nombreEspecialidad()) %></small></td>
            <td><%= h(pago.nombrePaciente()) %><br><small class="text-secondary"><%= h(pago.nombreMedico()) %></small></td>
            <td><%= h(pago.nombreMetodo()) %></td><td>Gs. <%= pago.monto().setScale(0).toPlainString() %></td>
            <td><span class="badge <%= pago.estaConfirmado() ? "text-bg-success" : (pago.estaPendiente() ? "text-bg-warning" : "text-bg-secondary") %>"><%= h(pago.nombreEstado()) %></span></td>
            <td><% if (pago.idComprobante() != null) { %><a target="_blank" href="${pageContext.request.contextPath}/comprobantes/pago?id=<%= pago.idComprobante() %>"><%= h(pago.numeroComprobante()) %></a><br><small class="text-secondary">Correo simulado</small><% } else { %><span class="text-secondary">Pendiente</span><% } %></td>
            <td><% if (pago.estaPendiente() && pago.esEfectivo()) { %><form method="post" action="${pageContext.request.contextPath}/agenda/pagos"><input type="hidden" name="accion" value="confirmarEfectivo"><input type="hidden" name="idPago" value="<%= pago.idPago() %>"><button class="btn btn-sm btn-success">Confirmar cobro</button></form><% } else if (pago.estaPendiente()) { %><small class="text-secondary">Espera al paciente</small><% } else { %><span class="text-secondary">—</span><% } %></td>
        </tr><% } %>
        </tbody></table></div><% } %>
    </div></section>
</main>
</body>
</html>
