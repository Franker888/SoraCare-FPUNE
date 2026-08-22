package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.PagoDAO;
import py.fpune.soracare.dao.NotificacionDAO;
import py.fpune.soracare.dto.ComprobantePagoDTO;
import py.fpune.soracare.dto.DatosFacturacionDTO;
import py.fpune.soracare.dto.FiltroAgendaPagosDTO;
import py.fpune.soracare.dto.PagoResumenDTO;
import py.fpune.soracare.dto.ResultadoAgendaPagosDTO;
import py.fpune.soracare.dto.SolicitudPagoDTO;
import py.fpune.soracare.mvc.model.Cita;
import py.fpune.soracare.mvc.model.MetodoPago;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class PagoService {

    private static final System.Logger LOGGER =
            System.getLogger(PagoService.class.getName());

    private static final Set<String> METODOS =
            Set.of("EFECTIVO", "QR", "TRANSFERENCIA", "TARJETA");
    private static final Set<String> ESTADOS_AGENDA =
            Set.of("PENDIENTE", "CONFIRMADO", "ANULADO", "REEMBOLSADO");
    private static final Set<String> TIPOS_DOCUMENTO =
            Set.of("CI", "RUC", "PASAPORTE");
    private static final Pattern CORREO = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private final PagoDAO pagoDAO = new PagoDAO();
    private final NotificacionDAO notificacionDAO = new NotificacionDAO();
    private final ComprobantePdfService comprobantePdfService = new ComprobantePdfService();
    private final CorreoResendService correoResendService = new CorreoResendService();

    public List<MetodoPago> listarMetodosActivos() throws SQLException {
        return pagoDAO.listarMetodosActivos();
    }

    PagoResumenDTO crearPendiente(
            Connection conexion,
            Cita cita,
            SolicitudPagoDTO solicitud,
            long idUsuario
    ) throws SQLException {
        if (cita == null || cita.getIdCita() == null) {
            throw new IllegalArgumentException("La cita es obligatoria para registrar el pago.");
        }
        SolicitudPagoDTO normalizada = normalizar(solicitud);
        BigDecimal tarifa = pagoDAO.buscarTarifa(
                conexion, cita.getIdMedico(), cita.getIdEspecialidad());
        String referencia = "SIM-" + normalizada.codigoMetodo() + "-"
                + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT);
        long idPago = pagoDAO.crearPendiente(
                conexion,
                cita.getIdCita(),
                normalizada.codigoMetodo(),
                tarifa,
                cita.getFechaHoraInicio(),
                referencia,
                "CITA-" + cita.getIdCita(),
                normalizada.datosFacturacion(),
                idUsuario
        );
        return pagoDAO.buscarPorId(conexion, idPago)
                .orElseThrow(() -> new SQLException("No fue posible recuperar el pago creado."));
    }

    public PagoResumenDTO buscarParaPaciente(long idPago, long idPaciente) throws SQLException {
        PagoResumenDTO pago = buscar(idPago);
        if (pago.idPaciente() != idPaciente) {
            throw new IllegalArgumentException("No podes consultar el pago de otro paciente.");
        }
        return pago;
    }

    public PagoResumenDTO buscar(long idPago) throws SQLException {
        if (idPago <= 0) throw new IllegalArgumentException("El pago seleccionado no es valido.");
        return pagoDAO.buscarPorId(idPago)
                .orElseThrow(() -> new IllegalArgumentException("El pago seleccionado no existe."));
    }

    public List<PagoResumenDTO> listarPorPaciente(long idPaciente) throws SQLException {
        if (idPaciente <= 0) throw new IllegalArgumentException("El paciente no es valido.");
        return pagoDAO.listarPorPaciente(idPaciente);
    }

    public List<PagoResumenDTO> listarRecientes() throws SQLException {
        return pagoDAO.listarRecientes();
    }

    public ResultadoAgendaPagosDTO buscarParaAgenda(FiltroAgendaPagosDTO filtro)
            throws SQLException {
        if (filtro == null) {
            throw new IllegalArgumentException("Los filtros de la agenda son obligatorios.");
        }
        String cedula = filtro.cedula().strip();
        if (!cedula.isBlank() && !cedula.matches("\\d{1,20}")) {
            throw new IllegalArgumentException(
                    "La cedula debe contener solamente numeros."
            );
        }
        if (!cedula.isBlank()) {
            try {
                long numeroCedula = Long.parseLong(cedula);
                if (numeroCedula <= 0) throw new NumberFormatException();
                cedula = Long.toString(numeroCedula);
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("La cedula ingresada no es valida.", error);
            }
        }
        String metodo = filtro.metodo().strip().toUpperCase(Locale.ROOT);
        if (!metodo.isBlank() && !METODOS.contains(metodo)) {
            throw new IllegalArgumentException("El metodo de pago seleccionado no es valido.");
        }
        String estado = filtro.estado().strip().toUpperCase(Locale.ROOT);
        if (!estado.isBlank() && !ESTADOS_AGENDA.contains(estado)) {
            throw new IllegalArgumentException("El estado de pago seleccionado no es valido.");
        }
        if (filtro.fechaDesde() != null && filtro.fechaHasta() != null
                && filtro.fechaDesde().isAfter(filtro.fechaHasta())) {
            throw new IllegalArgumentException(
                    "La fecha desde no puede ser posterior a la fecha hasta."
            );
        }
        if (filtro.tamanoPagina() > 100) {
            throw new IllegalArgumentException("El tamano de pagina solicitado no es valido.");
        }
        return pagoDAO.buscarParaAgenda(new FiltroAgendaPagosDTO(
                cedula, metodo, estado, filtro.fechaDesde(), filtro.fechaHasta(),
                filtro.pagina(), filtro.tamanoPagina()
        ));
    }

    public PagoResumenDTO confirmarDigital(
            long idPago,
            long idPaciente,
            long idUsuario,
            String detalle
    ) throws SQLException {
        return confirmar(idPago, idPaciente, idUsuario, false, detalle);
    }

    public PagoResumenDTO confirmarEfectivo(
            long idPago,
            long idUsuario
    ) throws SQLException {
        return confirmar(idPago, null, idUsuario, true, "Cobro registrado por recepcion");
    }

    private PagoResumenDTO confirmar(
            long idPago,
            Long idPaciente,
            long idUsuario,
            boolean exigirEfectivo,
            String detalle
    ) throws SQLException {
        if (idPago <= 0 || idUsuario <= 0) {
            throw new IllegalArgumentException("El pago seleccionado no es valido.");
        }
        String detalleNormalizado = normalizarTexto(detalle, 120,
                "Ingresa el dato solicitado para completar la simulacion.");

        PagoResumenDTO resultado;
        DatosFacturacionDTO facturacion;
        String numero;
        ComprobantePdfService.DocumentoPdf documento;

        try (Connection conexion = ConexionBD.obtenerConexion()) {
            try {
                conexion.setAutoCommit(false);
                pagoDAO.bloquear(conexion, idPago);
                PagoResumenDTO actual = pagoDAO.buscarPorId(conexion, idPago)
                        .orElseThrow(() -> new IllegalArgumentException("El pago seleccionado no existe."));
                if (idPaciente != null && actual.idPaciente() != idPaciente) {
                    throw new IllegalArgumentException("No podes procesar el pago de otro paciente.");
                }
                if (exigirEfectivo != actual.esEfectivo()) {
                    throw new IllegalArgumentException(exigirEfectivo
                            ? "Recepcion solo puede confirmar cobros pendientes en efectivo."
                            : "El pago en efectivo debe ser confirmado por recepcion.");
                }
                if (actual.estaConfirmado()) {
                    conexion.commit();
                    return actual;
                }
                if (!actual.estaPendiente()) {
                    throw new IllegalArgumentException(
                            "El pago ya no se encuentra pendiente y no puede procesarse.");
                }

                String referencia = actual.esEfectivo()
                        ? "CAJA-" + actual.idPago()
                        : limitar("PAGO-" + actual.idPago() + "-" + detalleNormalizado, 120);
                String datosProveedor = "{\"simulado\":true,\"detalle\":\""
                        + escaparJson(detalleNormalizado) + "\"}";
                pagoDAO.confirmar(conexion, idPago, actual.idCita(), idUsuario,
                        referencia, datosProveedor);

                PagoResumenDTO confirmado = pagoDAO.buscarPorId(conexion, idPago)
                        .orElseThrow(() -> new SQLException("No fue posible recuperar el pago confirmado."));
                facturacion = pagoDAO.buscarDatosFacturacion(conexion, idPago);
                numero = String.format("SC-%08d", idPago);
                documento =
                        comprobantePdfService.generar(confirmado, facturacion, numero);
                pagoDAO.crearComprobante(
                        conexion, idPago, idUsuario, numero, documento.nombreArchivo(),
                        documento.contenido(), documento.sha256(), facturacion.correo());
                notificacionDAO.registrarPagoConfirmado(
                        conexion,
                        confirmado.idCita(),
                        confirmado.idPago(),
                        numero,
                        "Tu pago de Gs. " + confirmado.monto().stripTrailingZeros().toPlainString()
                                + " fue confirmado. El comprobante " + numero
                                + " ya está disponible desde Mis citas."
                );

                resultado = pagoDAO.buscarPorId(conexion, idPago)
                        .orElseThrow(() -> new SQLException("No fue posible recuperar el comprobante."));
                conexion.commit();
            } catch (Exception error) {
                rollback(conexion, error);
                if (error instanceof IllegalArgumentException validacion) throw validacion;
                if (error instanceof SQLException sql) throw sql;
                throw new SQLException("No fue posible confirmar el pago.", error);
            }
        }

        programarEnvioComprobante(resultado, facturacion, numero, documento);
        return resultado;
    }

    private void programarEnvioComprobante(
            PagoResumenDTO pago,
            DatosFacturacionDTO facturacion,
            String numero,
            ComprobantePdfService.DocumentoPdf documento
    ) {
        if (!correoResendService.estaConfigurado()) {
            guardarEstadoCorreo(
                    pago.idPago(),
                    "NO_CONFIGURADO",
                    false,
                    null,
                    null,
                    "El comprobante esta disponible, pero Resend no esta configurado."
            );
            return;
        }

        DespachadorCorreoService.ejecutar(() -> {
            try {
                CorreoResendService.ResultadoEnvio envio = correoResendService.enviar(
                        facturacion.correo(),
                        "Comprobante " + numero + " - SoraCare-FPUNE",
                        contenidoCorreoComprobante(pago, numero),
                        List.of(new CorreoResendService.AdjuntoCorreo(
                                documento.nombreArchivo(),
                                documento.contenido()
                        )),
                        "pago-" + pago.idPago() + "-comprobante-v1"
                );
                guardarEstadoCorreo(
                        pago.idPago(),
                        "ENVIADO",
                        true,
                        "RESEND",
                        envio.referenciaProveedor(),
                        "Resend acepto el comprobante " + numero
                                + " para " + envio.destinatario() + "."
                );
            } catch (Exception error) {
                LOGGER.log(
                        System.Logger.Level.ERROR,
                        "No fue posible enviar el comprobante {0} por Resend.",
                        numero
                );
                guardarEstadoCorreo(
                        pago.idPago(),
                        "FALLIDO",
                        false,
                        "RESEND",
                        null,
                        "El comprobante " + numero
                                + " no pudo enviarse: " + limitarDetalle(error.getMessage())
                );
            }
        });
    }

    private void guardarEstadoCorreo(
            long idPago,
            String estado,
            boolean enviado,
            String proveedor,
            String referencia,
            String detalle
    ) {
        try {
            pagoDAO.registrarResultadoCorreo(
                    idPago,
                    estado,
                    enviado,
                    proveedor,
                    referencia,
                    detalle
            );
        } catch (SQLException error) {
            LOGGER.log(
                    System.Logger.Level.ERROR,
                    "No fue posible guardar el estado del correo del pago {0}.",
                    idPago
            );
        }
    }

    private static String contenidoCorreoComprobante(
            PagoResumenDTO pago,
            String numero
    ) {
        String fechaCita = pago.fechaCita()
                .atZoneSameInstant(DisponibilidadService.ZONA_HORARIA)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        return """
                <!doctype html>
                <html lang="es">
                <body style="margin:0;background:#f4f7fb;font-family:Arial,sans-serif;color:#10213a">
                  <div style="max-width:640px;margin:32px auto;background:#fff;border:1px solid #dbe5f1;border-radius:18px;overflow:hidden">
                    <div style="padding:28px 32px;background:linear-gradient(135deg,#2452c7,#159fc0);color:#fff">
                      <p style="margin:0 0 8px;font-size:13px;font-weight:700;letter-spacing:.08em">SORACARE-FPUNE</p>
                      <h1 style="margin:0;font-size:28px">Tu pago fue confirmado</h1>
                    </div>
                    <div style="padding:30px 32px">
                      <p>Hola, <strong>%s</strong>.</p>
                      <p>Adjuntamos el comprobante académico <strong>%s</strong> correspondiente a tu consulta.</p>
                      <div style="margin:24px 0;padding:18px;background:#eef5ff;border-radius:12px;line-height:1.7">
                        <strong>Especialidad:</strong> %s<br>
                        <strong>Profesional:</strong> %s<br>
                        <strong>Fecha:</strong> %s<br>
                        <strong>Importe:</strong> Gs. %s
                      </div>
                      <p style="color:#5b6b82;font-size:13px">Documento académico generado por SoraCare-FPUNE; no posee validez fiscal.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escaparHtml(pago.nombrePaciente()),
                escaparHtml(numero),
                escaparHtml(pago.nombreEspecialidad()),
                escaparHtml(pago.nombreMedico()),
                escaparHtml(fechaCita),
                pago.monto().setScale(0).toPlainString()
        );
    }

    private static String escaparHtml(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String limitarDetalle(String valor) {
        if (valor == null || valor.isBlank()) return "sin detalle del proveedor";
        String normalizado = valor.replace('\r', ' ').replace('\n', ' ').strip();
        return normalizado.length() <= 300
                ? normalizado
                : normalizado.substring(0, 300) + "...";
    }

    public ComprobantePagoDTO obtenerComprobante(
            long idComprobante,
            Long idPaciente,
            boolean personalAutorizado
    ) throws SQLException {
        if (idComprobante <= 0) {
            throw new IllegalArgumentException("El comprobante seleccionado no es valido.");
        }
        ComprobantePagoDTO comprobante = pagoDAO.buscarComprobante(idComprobante)
                .orElseThrow(() -> new IllegalArgumentException("El comprobante no existe."));
        if (!personalAutorizado
                && (idPaciente == null || comprobante.idPaciente() != idPaciente)) {
            throw new IllegalArgumentException("No podes descargar el comprobante de otro paciente.");
        }
        if (comprobante.contenidoPdf() == null || comprobante.contenidoPdf().length == 0) {
            throw new IllegalArgumentException("El comprobante todavia no posee un archivo disponible.");
        }
        return comprobante;
    }

    private static SolicitudPagoDTO normalizar(SolicitudPagoDTO solicitud) {
        if (solicitud == null || solicitud.datosFacturacion() == null) {
            throw new IllegalArgumentException("Selecciona un metodo e ingresa los datos de facturacion.");
        }
        String metodo = normalizarCodigo(solicitud.codigoMetodo());
        if (!METODOS.contains(metodo)) {
            throw new IllegalArgumentException("El metodo de pago seleccionado no es valido.");
        }
        DatosFacturacionDTO datos = solicitud.datosFacturacion();
        String tipoDocumento = normalizarCodigo(datos.tipoDocumento());
        if (!TIPOS_DOCUMENTO.contains(tipoDocumento)) {
            throw new IllegalArgumentException("El tipo de documento de facturacion no es valido.");
        }
        String numero = normalizarTexto(datos.numeroDocumento(), 30,
                "El numero de documento para facturacion es obligatorio.");
        String nombre = normalizarTexto(datos.nombreRazonSocial(), 160,
                "El nombre o razon social es obligatorio.");
        String direccion = normalizarTexto(datos.direccion(), 255,
                "La direccion para facturacion es obligatoria.");
        String correo = normalizarTexto(datos.correo(), 255,
                "El correo para recibir el comprobante es obligatorio.").toLowerCase(Locale.ROOT);
        if (!CORREO.matcher(correo).matches()) {
            throw new IllegalArgumentException("El correo de facturacion no posee un formato valido.");
        }
        return new SolicitudPagoDTO(metodo,
                new DatosFacturacionDTO(tipoDocumento, numero, nombre, direccion, correo));
    }

    private static String normalizarCodigo(String valor) {
        return valor == null ? "" : valor.strip().toUpperCase(Locale.ROOT);
    }

    private static String normalizarTexto(String valor, int maximo, String mensaje) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(mensaje);
        String texto = valor.strip();
        if (texto.length() > maximo) {
            throw new IllegalArgumentException("Uno de los datos ingresados supera el largo permitido.");
        }
        return texto;
    }

    private static String escaparJson(String valor) {
        return valor.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", " ").replace("\n", " ");
    }

    private static String limitar(String valor, int maximo) {
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }

    private static void rollback(Connection conexion, Exception original) {
        try {
            conexion.rollback();
        } catch (SQLException rollback) {
            original.addSuppressed(rollback);
        }
    }
}
