<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="py.fpune.soracare.dto.PagoResumenDTO" %>
<%@ page import="py.fpune.soracare.dto.ResumenFacturacionMedicoDTO" %>
<%@ page import="py.fpune.soracare.dto.EstadoPreparacionSifenDTO" %>
<%@ page import="py.fpune.soracare.dto.SolicitudSifenResumenDTO" %>
<%@ page import="py.fpune.soracare.service.DisponibilidadService" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.util.Map" %>
<%!
    private String h(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    ResumenFacturacionMedicoDTO resumen =
            request.getAttribute("resumenFacturacion") instanceof ResumenFacturacionMedicoDTO valor
                    ? valor
                    : ResumenFacturacionMedicoDTO.vacio();
    List<PagoResumenDTO> pagos = resumen.pagos();
    EstadoPreparacionSifenDTO estadoSifen =
            request.getAttribute("estadoSifen") instanceof EstadoPreparacionSifenDTO valorSifen
                    ? valorSifen
                    : new EstadoPreparacionSifenDTO(
                            EstadoPreparacionSifenDTO.Ambiente.DESHABILITADO,
                            true,
                            List.of()
                    );
    String csrfToken = (String) request.getAttribute("csrfToken");
    Map<Long, SolicitudSifenResumenDTO> solicitudesSifen =
            request.getAttribute("solicitudesSifen") instanceof Map<?, ?> mapaSifen
                    ? (Map<Long, SolicitudSifenResumenDTO>) mapaSifen
                    : Map.of();
    DateTimeFormatter fechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    NumberFormat guaranies = NumberFormat.getIntegerInstance(
            Locale.forLanguageTag("es-PY")
    );
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex,nofollow">
    <title>Facturaci&oacute;n interna | SoraCare-FPUNE</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/libs/bootstrap-icons/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css?v=20260831a">
    <style>
        .billing-hero { background: linear-gradient(135deg,#173f98,#168da6); color:#fff; border-radius:1.4rem; padding:2rem; }
        .billing-stat { border:0; border-radius:1rem; box-shadow:0 12px 32px rgba(25,55,95,.08); }
        .billing-table-card { border:0; border-radius:1.2rem; box-shadow:0 12px 36px rgba(25,55,95,.08); overflow:hidden; }
        .billing-warning { border-left:.3rem solid #e0a800; }
        .billing-number { font-family:ui-monospace,SFMono-Regular,Menlo,monospace; }
        .billing-filter { border:1px solid #dce6f2; border-radius:1rem; background:#fff; }
        .billing-filter .form-control, .billing-filter .form-select { min-height:2.8rem; }
        .billing-amount { white-space:nowrap; }
        .billing-no-results { display:none; }
        .sifen-form { min-width:18rem; text-align:left; }
        .sifen-cdc { max-width:21rem; overflow-wrap:anywhere; }
    </style>
</head>
<body class="app-shell">
<%@ include file="_navbar.jspf" %>
<main class="container py-4 py-lg-5">
    <section class="billing-hero mb-4">
        <div class="row align-items-center g-3">
            <div class="col">
                <p class="text-white-50 fw-semibold mb-2">Gestión comercial del profesional</p>
                <h1 class="h2 mb-2">Facturaci&oacute;n interna</h1>
                <p class="mb-0">Emití el documento comercial de una consulta con pago confirmado y compartilo en PDF con el paciente.</p>
            </div>
            <div class="col-md-auto d-flex align-items-center gap-3">
                <a class="btn btn-light fw-semibold <%= pagos.isEmpty() ? "disabled" : "" %>"
                   href="${pageContext.request.contextPath}/medico/facturas/exportar"
                   <%= pagos.isEmpty() ? "aria-disabled=\"true\" tabindex=\"-1\"" : "" %>>
                    <i class="bi bi-filetype-csv me-1" aria-hidden="true"></i>Exportar CSV
                </a>
                <i class="bi bi-receipt-cutoff display-3 d-none d-lg-block" aria-hidden="true"></i>
            </div>
        </div>
    </section>

    <div class="alert alert-warning billing-warning" role="alert">
        <strong>Alcance actual:</strong> estos documentos son registros internos sin validez tributaria.
        No reemplazan una factura electrónica de e-Kuatia/SIFEN ni incluyen timbrado o CDC.
    </div>

    <section class="card billing-table-card mb-4" aria-labelledby="tituloSifen">
        <div class="card-body p-4">
            <div class="d-flex flex-column flex-lg-row align-items-lg-start justify-content-between gap-3">
                <div>
                    <div class="d-flex align-items-center gap-2 mb-2">
                        <i class="bi bi-shield-check text-primary fs-4" aria-hidden="true"></i>
                        <h2 class="h5 mb-0" id="tituloSifen">Preparación e-Kuatia / SIFEN</h2>
                    </div>
                    <% if (!estadoSifen.ambienteValido()) { %>
                    <p class="text-danger mb-2">La variable del ambiente SIFEN tiene un valor no reconocido.</p>
                    <% } else if (!estadoSifen.habilitada()) { %>
                    <p class="text-secondary mb-2">
                        La integración fiscal está deshabilitada. Las facturas internas continúan funcionando con normalidad.
                    </p>
                    <% } else if (estadoSifen.configuracionCompleta() && estadoSifen.esProduccion()) { %>
                    <p class="text-warning mb-2">
                        La configuración de producción está completa, pero SoraCare mantiene bloqueadas la generación y transmisión fiscal desde este panel.
                    </p>
                    <% } else if (estadoSifen.configuracionCompleta()) { %>
                    <p class="text-success mb-2">
                        La configuración local está completa: SoraCare puede generar, firmar y validar el DE antes de transmitirlo.
                    </p>
                    <% } else { %>
                    <p class="text-secondary mb-2">
                        El ambiente está seleccionado, pero faltan <strong><%= estadoSifen.requisitosPendientes().size() %></strong>
                        requisitos de configuración.
                    </p>
                    <% } %>

                    <% if (!estadoSifen.requisitosPendientes().isEmpty()) { %>
                    <ul class="small text-secondary mb-2">
                        <% for (String requisito : estadoSifen.requisitosPendientes()) { %>
                        <li><%= h(requisito) %></li>
                        <% } %>
                    </ul>
                    <% } %>
                    <p class="small text-secondary mb-0">
                        Validar localmente no convierte el DE en DTE. La validez tributaria comienza únicamente cuando SIFEN lo aprueba.
                    </p>
                </div>
                <div class="d-flex flex-column align-items-lg-end gap-2">
                    <% if (!estadoSifen.ambienteValido()) { %>
                    <span class="badge text-bg-danger">Configuración inválida</span>
                    <% } else if (!estadoSifen.habilitada()) { %>
                    <span class="badge text-bg-secondary">Deshabilitado</span>
                    <% } else if (estadoSifen.configuracionCompleta() && estadoSifen.esProduccion()) { %>
                    <span class="badge text-bg-warning">PRODUCCION protegida</span>
                    <% } else if (estadoSifen.configuracionCompleta()) { %>
                    <span class="badge text-bg-success"><%= h(estadoSifen.ambiente().name()) %> configurado</span>
                    <% } else { %>
                    <span class="badge text-bg-warning"><%= h(estadoSifen.ambiente().name()) %> incompleto</span>
                    <% } %>
                    <a class="btn btn-outline-primary btn-sm" target="_blank" rel="noopener noreferrer"
                       href="https://www.dnit.gov.py/web/e-kuatia/documentacion-tecnica">
                        Documentación oficial DNIT
                    </a>
                </div>
            </div>
        </div>
    </section>

    <% if (request.getAttribute("mensajeExito") != null) { %>
    <div class="alert alert-success"><%= h((String) request.getAttribute("mensajeExito")) %></div>
    <% } %>
    <% if (request.getAttribute("error") != null) { %>
    <div class="alert alert-danger"><%= h((String) request.getAttribute("error")) %></div>
    <% } %>

    <section class="row g-3 mb-4" aria-label="Resumen de facturación">
        <div class="col-sm-6 col-xl-3">
            <article class="card billing-stat h-100"><div class="card-body p-4">
                <div class="text-secondary small">Pagos confirmados</div>
                <div class="display-6 fw-bold"><%= pagos.size() %></div>
            </div></article>
        </div>
        <div class="col-sm-6 col-xl-3">
            <article class="card billing-stat h-100"><div class="card-body p-4">
                <div class="text-secondary small">Facturas emitidas</div>
                <div class="display-6 fw-bold text-success"><%= resumen.facturasEmitidas() %></div>
            </div></article>
        </div>
        <div class="col-sm-6 col-xl-3">
            <article class="card billing-stat h-100"><div class="card-body p-4">
                <div class="text-secondary small">Pendientes de emisión</div>
                <div class="display-6 fw-bold text-primary"><%= resumen.pendientesEmision() %></div>
            </div></article>
        </div>
        <div class="col-sm-6 col-xl-3">
            <article class="card billing-stat h-100"><div class="card-body p-4">
                <div class="text-secondary small">Total facturado</div>
                <div class="h3 fw-bold text-primary billing-amount mb-0">
                    Gs. <%= guaranies.format(resumen.totalFacturado()) %>
                </div>
                <div class="text-secondary small mt-1">
                    de Gs. <%= guaranies.format(resumen.totalCobrado()) %> cobrados
                </div>
            </div></article>
        </div>
    </section>

    <% if (!pagos.isEmpty()) { %>
    <section class="billing-filter p-3 p-lg-4 mb-4" aria-label="Filtros de facturación">
        <div class="row g-3 align-items-end">
            <div class="col-lg-8">
                <label class="form-label fw-semibold" for="filtroFacturacion">Buscar</label>
                <div class="input-group">
                    <span class="input-group-text bg-white"><i class="bi bi-search" aria-hidden="true"></i></span>
                    <input class="form-control" id="filtroFacturacion" type="search"
                           placeholder="Paciente, cédula, especialidad o número de factura">
                </div>
            </div>
            <div class="col-lg-4">
                <label class="form-label fw-semibold" for="estadoFacturacion">Estado</label>
                <select class="form-select" id="estadoFacturacion">
                    <option value="TODAS">Todas</option>
                    <option value="PENDIENTE">Pendientes</option>
                    <option value="EMITIDA">Emitidas</option>
                </select>
            </div>
        </div>
        <div class="small text-secondary mt-3" aria-live="polite" id="resultadoFiltro"></div>
    </section>
    <% } %>

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
                    <%
                        SolicitudSifenResumenDTO solicitudSifen = pago.idFacturaInterna() == null
                                ? null
                                : solicitudesSifen.get(pago.idFacturaInterna());
                    %>
                    <tr class="billing-row"
                        data-estado="<%= pago.idFacturaInterna() == null ? "PENDIENTE" : "EMITIDA" %>"
                        data-busqueda="<%= h((pago.nombrePaciente() + " "
                                + pago.cedulaPaciente() + " " + pago.nombreEspecialidad() + " "
                                + (pago.numeroFacturaInterna() == null ? "" : pago.numeroFacturaInterna()))) %>">
                        <td class="ps-4">
                            <strong><%= h(pago.nombrePaciente()) %></strong>
                            <div class="text-secondary small"><%= h(pago.nombreEspecialidad()) %> · CI <%= h(pago.cedulaPaciente()) %></div>
                        </td>
                        <td><%= pago.fechaCita().atZoneSameInstant(DisponibilidadService.ZONA_HORARIA).format(fechaHora) %></td>
                        <td>Gs. <strong><%= guaranies.format(pago.monto()) %></strong></td>
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
                            <% if (solicitudSifen != null) { %>
                            <div class="mt-2">
                                <% if (solicitudSifen.aprobada()) { %>
                                <span class="badge text-bg-success">SIFEN aprobado</span>
                                <% } else if (solicitudSifen.rechazada()) { %>
                                <span class="badge text-bg-danger">SIFEN rechazado</span>
                                <% } else if (solicitudSifen.validadaLocalmente()) { %>
                                <span class="badge text-bg-primary">DE validado localmente</span>
                                <% } else { %>
                                <span class="badge text-bg-secondary"><%= h(solicitudSifen.estado()) %></span>
                                <% } %>
                                <div class="small text-secondary billing-number mt-1">
                                    N.º fiscal <%= h(solicitudSifen.numeroDocumento()) %>
                                </div>
                                <div class="small text-secondary billing-number sifen-cdc mt-1"
                                     title="CDC completo">
                                    CDC <%= h(solicitudSifen.cdc()) %>
                                </div>
                                <% if (solicitudSifen.codigoRespuesta() != null) { %>
                                <div class="small mt-1 <%= solicitudSifen.aprobada() ? "text-success" : "text-danger" %>">
                                    Código <%= h(solicitudSifen.codigoRespuesta()) %>
                                    <% if (solicitudSifen.mensajeRespuesta() != null) { %>
                                    · <%= h(solicitudSifen.mensajeRespuesta()) %>
                                    <% } %>
                                </div>
                                <% } %>
                            </div>
                            <% } %>
                        </td>
                        <td class="pe-4 text-end">
                            <% if (pago.idFacturaInterna() == null) { %>
                            <form method="post" action="${pageContext.request.contextPath}/medico/facturas" class="d-inline">
                                <input type="hidden" name="csrfToken" value="<%= h(csrfToken) %>">
                                <input type="hidden" name="idPago" value="<%= pago.idPago() %>">
                                <button class="btn btn-primary btn-sm" type="submit">
                                    <i class="bi bi-file-earmark-plus me-1"></i>Emitir factura
                                </button>
                            </form>
                            <% } else { %>
                            <div class="d-inline-flex flex-column align-items-end gap-2">
                                <a class="btn btn-outline-primary btn-sm" target="_blank"
                                   href="${pageContext.request.contextPath}/facturas/interna?id=<%= pago.idFacturaInterna() %>">
                                    <i class="bi bi-file-earmark-pdf me-1"></i>Ver PDF
                                </a>
                                <% if (solicitudSifen != null
                                        && "PRUEBA".equals(solicitudSifen.ambiente())
                                        && (solicitudSifen.aprobada() || "VALIDADO".equals(solicitudSifen.estado()))) { %>
                                <a class="btn btn-outline-secondary btn-sm" target="_blank" rel="noopener"
                                   href="${pageContext.request.contextPath}/facturas/kude?id=<%= solicitudSifen.idSolicitud() %>">
                                    <i class="bi bi-file-earmark-pdf me-1"></i>KuDE de prueba
                                </a>
                                <span class="small text-secondary">Sin validez fiscal</span>
                                <% } %>
                                <% if (solicitudSifen == null
                                        && estadoSifen.configuracionCompleta()
                                        && estadoSifen.ambiente() == EstadoPreparacionSifenDTO.Ambiente.PRUEBA) { %>
                                <details class="sifen-form border rounded-3 p-2 bg-light">
                                    <summary class="small fw-semibold text-primary">Preparar DE para SIFEN</summary>
                                    <form method="post" action="${pageContext.request.contextPath}/medico/facturas"
                                          class="mt-3">
                                        <input type="hidden" name="csrfToken" value="<%= h(csrfToken) %>">
                                        <input type="hidden" name="accion" value="prepararSifen">
                                        <input type="hidden" name="idFactura" value="<%= pago.idFacturaInterna() %>">
                                        <label class="form-label small fw-semibold" for="iva-<%= pago.idPago() %>">IVA</label>
                                        <select class="form-select form-select-sm mb-2" id="iva-<%= pago.idPago() %>"
                                                name="tratamientoIva" required>
                                            <option value="" selected disabled>Seleccionar</option>
                                            <option value="GRAVADO_10">Gravado 10 %</option>
                                            <option value="GRAVADO_5">Gravado 5 %</option>
                                            <option value="EXENTO">Exento</option>
                                        </select>
                                        <label class="form-label small fw-semibold" for="pago-<%= pago.idPago() %>">Forma de pago fiscal</label>
                                        <select class="form-select form-select-sm mb-2" id="pago-<%= pago.idPago() %>"
                                                name="tipoPagoSifen" required>
                                            <option value="" selected disabled>Seleccionar</option>
                                            <option value="1">Efectivo</option>
                                            <option value="5">Transferencia</option>
                                        </select>
                                        <label class="form-label small fw-semibold" for="receptor-<%= pago.idPago() %>">Tipo de contribuyente</label>
                                        <select class="form-select form-select-sm mb-3"
                                                id="receptor-<%= pago.idPago() %>"
                                                name="tipoContribuyenteReceptor">
                                            <option value="1">Persona física</option>
                                            <option value="2">Persona jurídica</option>
                                        </select>
                                        <p class="small text-secondary mt-n2 mb-2">
                                            Solo se aplica cuando el paciente informó RUC.
                                        </p>
                                        <p class="small text-secondary mb-2">
                                            Genera, firma y valida localmente. No envía a la DNIT.
                                        </p>
                                        <button class="btn btn-primary btn-sm w-100" type="submit">
                                            <i class="bi bi-shield-check me-1"></i>Generar y validar DE
                                        </button>
                                    </form>
                                </details>
                                <% } %>
                                <% if (solicitudSifen != null
                                        && "PRUEBA".equals(solicitudSifen.ambiente())
                                        && ("VALIDADO".equals(solicitudSifen.estado())
                                            || "ERROR".equals(solicitudSifen.estado()))) { %>
                                <form method="post" action="${pageContext.request.contextPath}/medico/facturas"
                                      onsubmit="return confirm('¿Enviar este DE al ambiente de PRUEBA de SIFEN?');">
                                    <input type="hidden" name="csrfToken" value="<%= h(csrfToken) %>">
                                    <input type="hidden" name="accion" value="enviarSifenPrueba">
                                    <input type="hidden" name="idSolicitud" value="<%= solicitudSifen.idSolicitud() %>">
                                    <button class="btn btn-outline-success btn-sm" type="submit">
                                        <i class="bi bi-cloud-arrow-up me-1"></i>
                                        <%= "ERROR".equals(solicitudSifen.estado()) ? "Reintentar en SIFEN Prueba" : "Enviar a SIFEN Prueba" %>
                                    </button>
                                </form>
                                <% } %>
                            </div>
                            <% } %>
                        </td>
                    </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
            <div class="billing-no-results p-5 text-center text-secondary" id="sinResultados">
                <i class="bi bi-search display-6 d-block mb-2" aria-hidden="true"></i>
                No hay registros que coincidan con los filtros seleccionados.
            </div>
            <% } %>
        </div>
    </section>
</main>
<script src="${pageContext.request.contextPath}/assets/libs/bootstrap/bootstrap.bundle.min.js"></script>
<script>
    (() => {
        const buscador = document.getElementById('filtroFacturacion');
        const estado = document.getElementById('estadoFacturacion');
        const filas = Array.from(document.querySelectorAll('.billing-row'));
        const resultado = document.getElementById('resultadoFiltro');
        const sinResultados = document.getElementById('sinResultados');
        if (!buscador || !estado || filas.length === 0) return;

        const normalizar = valor => (valor || '')
            .normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
        const filtrar = () => {
            const texto = normalizar(buscador.value);
            const estadoElegido = estado.value;
            let visibles = 0;
            filas.forEach(fila => {
                const coincideTexto = !texto
                    || normalizar(fila.dataset.busqueda).includes(texto);
                const coincideEstado = estadoElegido === 'TODAS'
                    || fila.dataset.estado === estadoElegido;
                const visible = coincideTexto && coincideEstado;
                fila.hidden = !visible;
                if (visible) visibles++;
            });
            resultado.textContent = visibles + (visibles === 1
                ? ' registro visible' : ' registros visibles');
            sinResultados.style.display = visibles === 0 ? 'block' : 'none';
        };
        buscador.addEventListener('input', filtrar);
        estado.addEventListener('change', filtrar);
        filtrar();
    })();
</script>
</body>
</html>
