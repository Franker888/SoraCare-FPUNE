<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.PagoResumenDTO" %>
<%@ page import="py.fpune.soracare.service.DisponibilidadService" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.util.Locale" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    PagoResumenDTO pago = (PagoResumenDTO) request.getAttribute("pago");
    DateTimeFormatter fechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    NumberFormat formatoImporte = NumberFormat.getIntegerInstance(new Locale("es", "PY"));
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>Pago simulado | SoraCare-FPUNE</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { background: #f3f8f6; }
        .barra { background: #0f766e; }
        .tarjeta { border: 0; border-radius: 1rem; }
        .tarjeta-demo { background: linear-gradient(135deg, #123d34, #0f766e); border-radius: 1rem; color: white; }
        .marca-tarjeta { letter-spacing: .04em; }
        .qr-demo { display: grid; grid-template-columns: repeat(9, 13px); gap: 2px; width: max-content; padding: 14px; background: white; border: 1px solid #d8e4df; }
        .qr-demo span { width: 13px; height: 13px; background: #d5e2dc; }
        .qr-demo span:nth-child(2n), .qr-demo span:nth-child(5n), .qr-demo span:nth-child(11n) { background: #123d34; }
    </style>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css">
</head>
<body class="app-shell app-patient">
<nav class="navbar barra shadow-sm"><div class="container py-2">
    <a class="navbar-brand fw-bold text-white" href="${pageContext.request.contextPath}/mis-citas">SoraCare-FPUNE</a>
    <a class="btn btn-outline-light btn-sm" href="${pageContext.request.contextPath}/mis-citas">Volver a mis citas</a>
</div></nav>
<main class="container py-4 py-md-5" style="max-width: 900px">
    <div class="mb-4"><p class="text-success fw-semibold mb-1">Etapa 5</p>
        <h1 class="h2">Pago simulado</h1>
        <p class="text-secondary">Este entorno no se conecta con bancos, procesadoras ni tarjetas reales.</p>
    </div>
    <% if (request.getAttribute("mensajeExito") != null) { %><div class="alert alert-success"><%= h((String) request.getAttribute("mensajeExito")) %></div><% } %>
    <% if (request.getAttribute("error") != null) { %><div class="alert alert-danger"><%= h((String) request.getAttribute("error")) %></div><% } %>

    <section class="card tarjeta shadow-sm mb-4"><div class="card-body p-4">
        <div class="row g-3">
            <div class="col-md-7"><small class="text-secondary">Consulta</small><h2 class="h5"><%= h(pago.nombreEspecialidad()) %></h2>
                <div><%= h(pago.nombreMedico()) %></div>
                <div class="text-secondary"><%= pago.fechaCita().atZoneSameInstant(DisponibilidadService.ZONA_HORARIA).format(fechaHora) %></div></div>
            <div class="col-md-5 text-md-end"><small class="text-secondary">Importe</small>
                <div class="display-6 fw-semibold">Gs. <%= pago.monto().setScale(0).toPlainString() %></div>
                <span class="badge <%= pago.estaConfirmado() ? "text-bg-success" : "text-bg-warning" %>"><%= h(pago.nombreEstado()) %></span></div>
        </div>
    </div></section>

    <% if (pago.estaPendiente()) { %>
    <section class="card tarjeta shadow-sm"><div class="card-body p-4">
        <h2 class="h5 mb-3"><%= h(pago.nombreMetodo()) %></h2>
        <form method="post" action="${pageContext.request.contextPath}/pagos/procesar" class="row g-3">
            <input type="hidden" name="idPago" value="<%= pago.idPago() %>">
            <% if ("QR".equals(pago.codigoMetodo())) { %>
            <div class="col-md-5"><div class="qr-demo" aria-label="Código QR de demostración">
                <% for (int i = 0; i < 81; i++) { %><span></span><% } %>
            </div><small class="text-secondary">QR visual de demostración; no puede cobrar dinero.</small></div>
            <div class="col-md-7"><label class="form-label" for="codigoConfirmacion">Código de confirmación de prueba</label>
                <input class="form-control" id="codigoConfirmacion" name="codigoConfirmacion" required maxlength="100" value="QR-DEMO-OK">
                <div class="form-text">Usá el código de prueba mostrado. No abras una aplicación bancaria.</div></div>
            <% } else if ("TRANSFERENCIA".equals(pago.codigoMetodo())) { %>
            <div class="col-12">
                <div class="card bg-light border-0">
                    <div class="card-body p-4">
                        <div class="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-3">
                            <div>
                                <small class="text-success fw-semibold text-uppercase">Datos para transferir</small>
                                <h3 class="h5 mb-0">Cuenta académica SoraCare</h3>
                            </div>
                            <span class="badge text-bg-secondary">Solo simulación</span>
                        </div>
                        <div class="row g-3">
                            <div class="col-md-6">
                                <small class="text-secondary d-block">Banco</small>
                                <strong>Banco Demo SoraCare</strong>
                            </div>
                            <div class="col-md-6">
                                <small class="text-secondary d-block">Titular</small>
                                <strong>SoraCare-FPUNE</strong>
                            </div>
                            <div class="col-md-6">
                                <small class="text-secondary d-block">Tipo y moneda</small>
                                <strong>Cuenta corriente · Guaraníes</strong>
                            </div>
                            <div class="col-md-6">
                                <small class="text-secondary d-block">Número de cuenta</small>
                                <strong class="user-select-all">000123456</strong>
                            </div>
                            <div class="col-md-6">
                                <small class="text-secondary d-block">Importe exacto</small>
                                <strong>Gs. <%= formatoImporte.format(pago.monto()) %></strong>
                            </div>
                            <div class="col-md-6">
                                <small class="text-secondary d-block">Concepto de la transferencia</small>
                                <strong class="user-select-all"><%= h(pago.referenciaExterna()) %></strong>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-12">
                <label class="form-label" for="referenciaTransferencia">Código de confirmación de prueba</label>
                <input class="form-control" id="referenciaTransferencia" name="referenciaTransferencia"
                       required maxlength="100" value="TRANS-DEMO-OK">
                <div class="form-text">
                    En esta simulación, este código representa la confirmación que entregaría el banco.
                </div>
            </div>
            <% } else if ("TARJETA".equals(pago.codigoMetodo())) { %>
            <div class="col-12">
                <div class="tarjeta-demo p-4">
                    <div class="d-flex justify-content-between align-items-start gap-3">
                        <div>
                            <small class="opacity-75">Tarjeta de prueba</small>
                            <div class="fs-5 mt-3" id="numeroVista">4242 4242 4242 4242</div>
                        </div>
                        <strong class="marca-tarjeta" id="marcaTarjeta">VISA</strong>
                    </div>
                    <div class="d-flex justify-content-between mt-4">
                        <div><small class="opacity-75 d-block">Titular</small><span id="titularVista">PACIENTE DE PRUEBA</span></div>
                        <div><small class="opacity-75 d-block">Vence</small><span id="vencimientoVista">12/30</span></div>
                    </div>
                </div>
            </div>
            <div class="col-12">
                <label class="form-label" for="numeroTarjeta">Número de tarjeta</label>
                <div class="input-group">
                    <input class="form-control" id="numeroTarjeta" name="numeroTarjeta"
                           inputmode="numeric" autocomplete="off" maxlength="19" required
                           value="4242 4242 4242 4242" aria-describedby="ayudaTarjeta">
                    <span class="input-group-text" id="marcaDetectada">Visa</span>
                </div>
                <div class="form-text" id="ayudaTarjeta">
                    Tarjetas ficticias habilitadas: Visa 4242 4242 4242 4242 y Mastercard 5555 5555 5555 4444.
                </div>
            </div>
            <div class="col-md-6">
                <label class="form-label" for="nombreTitularTarjeta">Nombre del titular</label>
                <input class="form-control" id="nombreTitularTarjeta" name="nombreTitularTarjeta"
                       autocomplete="off" maxlength="80" required value="PACIENTE DE PRUEBA">
            </div>
            <div class="col-md-3">
                <label class="form-label" for="vencimientoTarjeta">Vencimiento</label>
                <input class="form-control" id="vencimientoTarjeta" name="vencimientoTarjeta"
                       inputmode="numeric" autocomplete="off" maxlength="5" placeholder="MM/AA"
                       pattern="(0[1-9]|1[0-2])/[0-9]{2}" required value="12/30">
            </div>
            <div class="col-md-3">
                <label class="form-label" for="codigoSeguridadTarjeta">CVV/CVC</label>
                <input class="form-control" type="password" id="codigoSeguridadTarjeta"
                       name="codigoSeguridadTarjeta" inputmode="numeric" autocomplete="off"
                       minlength="3" maxlength="3" pattern="[0-9]{3}" required value="123">
            </div>
            <div class="col-12">
                <small class="text-secondary">Esta simulación acepta únicamente los datos ficticios indicados.</small>
            </div>
            <% } %>
            <div class="col-12"><button class="btn btn-success">Realizar Pago</button></div>
        </form>
    </div></section>
    <% } else { %>
    <section class="card tarjeta shadow-sm"><div class="card-body p-4">
        <h2 class="h5">Operación finalizada</h2>
        <p class="text-secondary">Referencia: <%= h(pago.referenciaExterna()) %></p>
        <% if (pago.idComprobante() != null) { %>
        <a class="btn btn-success" target="_blank" href="${pageContext.request.contextPath}/comprobantes/pago?id=<%= pago.idComprobante() %>">Abrir comprobante PDF</a>
        <% } %>
        <a class="btn btn-outline-success" href="${pageContext.request.contextPath}/mis-citas">Ver mis citas</a>
        <div class="alert alert-info mt-3 mb-0">El envío a <strong><%= h(pago.correoFacturacion()) %></strong> quedó registrado como simulación local.</div>
    </div></section>
    <% } %>
</main>
<% if (pago.estaPendiente() && "TARJETA".equals(pago.codigoMetodo())) { %>
<script>
    const numeroTarjeta = document.getElementById('numeroTarjeta');
    const titularTarjeta = document.getElementById('nombreTitularTarjeta');
    const vencimientoTarjeta = document.getElementById('vencimientoTarjeta');
    const marcaTarjeta = document.getElementById('marcaTarjeta');
    const marcaDetectada = document.getElementById('marcaDetectada');
    const numeroVista = document.getElementById('numeroVista');
    const titularVista = document.getElementById('titularVista');
    const vencimientoVista = document.getElementById('vencimientoVista');

    function detectarMarca(numero) {
        if (numero.startsWith('4')) return 'VISA';
        const inicioDosDigitos = Number(numero.substring(0, 2));
        const inicioCuatroDigitos = Number(numero.substring(0, 4));
        if ((inicioDosDigitos >= 51 && inicioDosDigitos <= 55)
                || (inicioCuatroDigitos >= 2221 && inicioCuatroDigitos <= 2720)) {
            return 'MASTERCARD';
        }
        return 'NO DETECTADA';
    }

    numeroTarjeta.addEventListener('input', () => {
        const digitos = numeroTarjeta.value.replace(/\D/g, '').substring(0, 16);
        numeroTarjeta.value = digitos.replace(/(.{4})/g, '$1 ').trim();
        const marca = detectarMarca(digitos);
        marcaTarjeta.textContent = marca;
        marcaDetectada.textContent = marca === 'NO DETECTADA' ? 'Sin detectar' :
                marca.charAt(0) + marca.substring(1).toLowerCase();
        numeroVista.textContent = numeroTarjeta.value || '0000 0000 0000 0000';
    });

    titularTarjeta.addEventListener('input', () => {
        titularVista.textContent = titularTarjeta.value.trim().toUpperCase() || 'TITULAR';
    });

    vencimientoTarjeta.addEventListener('input', () => {
        const digitos = vencimientoTarjeta.value.replace(/\D/g, '').substring(0, 4);
        vencimientoTarjeta.value = digitos.length > 2
                ? digitos.substring(0, 2) + '/' + digitos.substring(2)
                : digitos;
        vencimientoVista.textContent = vencimientoTarjeta.value || 'MM/AA';
    });
</script>
<% } %>
</body>
</html>
