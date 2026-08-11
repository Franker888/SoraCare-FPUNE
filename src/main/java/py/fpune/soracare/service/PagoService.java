package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.PagoDAO;
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
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class PagoService {

    private static final Set<String> METODOS =
            Set.of("EFECTIVO", "QR", "TRANSFERENCIA", "TARJETA");
    private static final Set<String> ESTADOS_AGENDA =
            Set.of("PENDIENTE", "CONFIRMADO", "ANULADO", "REEMBOLSADO");
    private static final Set<String> TIPOS_DOCUMENTO =
            Set.of("CI", "RUC", "PASAPORTE");
    private static final Pattern CORREO = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private final PagoDAO pagoDAO = new PagoDAO();
    private final ComprobantePdfService comprobantePdfService = new ComprobantePdfService();

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
                DatosFacturacionDTO facturacion = pagoDAO.buscarDatosFacturacion(conexion, idPago);
                String numero = String.format("SC-%08d", idPago);
                ComprobantePdfService.DocumentoPdf documento =
                        comprobantePdfService.generar(confirmado, facturacion, numero);
                pagoDAO.crearComprobante(
                        conexion, idPago, idUsuario, numero, documento.nombreArchivo(),
                        documento.contenido(), documento.sha256(), facturacion.correo());
                pagoDAO.registrarCorreoSimulado(conexion, confirmado, numero);

                PagoResumenDTO resultado = pagoDAO.buscarPorId(conexion, idPago)
                        .orElseThrow(() -> new SQLException("No fue posible recuperar el comprobante."));
                conexion.commit();
                return resultado;
            } catch (Exception error) {
                rollback(conexion, error);
                if (error instanceof IllegalArgumentException validacion) throw validacion;
                if (error instanceof SQLException sql) throw sql;
                throw new SQLException("No fue posible confirmar el pago.", error);
            }
        }
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
