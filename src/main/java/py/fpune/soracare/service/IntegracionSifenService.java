package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.SolicitudSifenDAO;
import py.fpune.soracare.dto.FacturaParaSifenDTO;
import py.fpune.soracare.dto.DocumentoSifenEnvioDTO;
import py.fpune.soracare.dto.SolicitudSifenResumenDTO;
import py.fpune.soracare.sifen.AfectacionIvaSifen;
import py.fpune.soracare.sifen.CertificadoSifenService;
import py.fpune.soracare.sifen.ClienteSoapSifenService;
import py.fpune.soracare.sifen.ConfiguracionFiscalSifen;
import py.fpune.soracare.sifen.DocumentoElectronicoSifen;
import py.fpune.soracare.sifen.FirmaXmlSifenService;
import py.fpune.soracare.sifen.ItemDocumentoSifen;
import py.fpune.soracare.sifen.QrSifenService;
import py.fpune.soracare.sifen.ReceptorSifen;
import py.fpune.soracare.sifen.ValidadorXsdSifenService;
import py.fpune.soracare.sifen.XmlDocumentoSifenService;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Prepara, firma y valida localmente un DE. La transmisión a la DNIT es una
 * acción posterior y nunca ocurre como efecto lateral de este método.
 */
public class IntegracionSifenService {

    private final SolicitudSifenDAO solicitudDAO = new SolicitudSifenDAO();
    private final ConfiguracionSifenService configuracionService =
            new ConfiguracionSifenService();
    private final XmlDocumentoSifenService xmlService =
            new XmlDocumentoSifenService();
    private final CertificadoSifenService certificadoService =
            new CertificadoSifenService();
    private final FirmaXmlSifenService firmaService =
            new FirmaXmlSifenService();
    private final QrSifenService qrService = new QrSifenService();
    private final ValidadorXsdSifenService validadorService =
            new ValidadorXsdSifenService();
    private final ClienteSoapSifenService clienteSoap =
            new ClienteSoapSifenService();
    private final SecureRandom aleatorio = new SecureRandom();

    public Map<Long, SolicitudSifenResumenDTO> listarParaMedico(
            long idPersonaMedico
    ) throws SQLException {
        validarId(idPersonaMedico, "La cuenta médica no es válida.");
        return solicitudDAO.listarPorMedico(idPersonaMedico);
    }

    public SolicitudSifenResumenDTO prepararYFirmar(
            long idFactura,
            long idPersonaMedico,
            String tratamientoIva,
            int tipoPago,
            int tipoContribuyenteReceptor
    ) throws SQLException {
        validarId(idFactura, "La factura interna seleccionada no es válida.");
        validarId(idPersonaMedico, "La cuenta médica no es válida.");
        OpcionIva opcionIva = OpcionIva.desde(tratamientoIva);
        DatosPago pago = DatosPago.desde(tipoPago);

        try (ConfiguracionFiscalSifen configuracion =
                     configuracionService.cargarConfiguracion();
             Connection conexion = ConexionBD.obtenerConexion()) {
            if (configuracion.produccion()) {
                throw new IllegalArgumentException(
                        "La generación fiscal desde esta pantalla está limitada a PRUEBA. Producción permanece bloqueada hasta completar la homologación y una habilitación operativa separada."
                );
            }
            try {
                conexion.setAutoCommit(false);
                FacturaParaSifenDTO factura = solicitudDAO.buscarFacturaDelMedico(
                                conexion, idFactura, idPersonaMedico
                        )
                        .orElseThrow(() -> new IllegalArgumentException(
                                "La factura no existe o no pertenece a este profesional."
                        ));
                validarFactura(factura);

                SolicitudSifenResumenDTO existente = solicitudDAO
                        .buscarPorFactura(conexion, idFactura)
                        .orElse(null);
                if (existente != null) {
                    conexion.commit();
                    return existente;
                }

                String numero = solicitudDAO.reservarNumero(
                        conexion,
                        configuracion.ambiente().name(),
                        configuracion.establecimiento(),
                        configuracion.puntoExpedicion(),
                        ""
                );
                String codigoSeguridad = codigoSeguridad();
                DocumentoElectronicoSifen documento = new DocumentoElectronicoSifen(
                        configuracion.timbrado(),
                        configuracion.fechaInicioTimbrado(),
                        configuracion.establecimiento(),
                        configuracion.puntoExpedicion(),
                        numero,
                        "",
                        codigoSeguridad,
                        fechaLocal(factura),
                        configuracion.emisor(),
                        receptor(factura, tipoContribuyenteReceptor),
                        items(factura, opcionIva),
                        pago.codigo(),
                        pago.descripcion(),
                        configuracion.informacionEmisor()
                );

                var sinFirma = xmlService.generar(documento);
                byte[] firmado = firmar(sinFirma.contenido(), configuracion);
                var conQr = agregarQr(firmado, configuracion);
                validadorService.validar(conQr.contenido());
                String hash = sha256(conQr.contenido());

                SolicitudSifenResumenDTO guardada = solicitudDAO.guardarValidada(
                        conexion,
                        idFactura,
                        configuracion.ambiente().name(),
                        configuracion.establecimiento(),
                        configuracion.puntoExpedicion(),
                        numero,
                        codigoSeguridad,
                        sinFirma.cdc(),
                        opcionIva.afectacion(),
                        opcionIva.tasa(),
                        sinFirma.calculoFiscal(),
                        sinFirma.contenido(),
                        conQr.contenido(),
                        conQr.urlQr(),
                        hash
                );
                conexion.commit();
                return guardada;
            } catch (Exception error) {
                rollback(conexion, error);
                if (error instanceof IllegalArgumentException validacion) {
                    throw validacion;
                }
                if (error instanceof IllegalStateException estado) {
                    throw new IllegalArgumentException(estado.getMessage(), estado);
                }
                if (error instanceof SQLException sql) throw sql;
                throw new SQLException(
                        "No fue posible preparar y firmar el documento SIFEN.", error
                );
            }
        }
    }

    public SolicitudSifenResumenDTO enviarAPruebas(
            long idSolicitud,
            long idPersonaMedico
    ) throws SQLException {
        validarId(idSolicitud, "El documento SIFEN seleccionado no es válido.");
        validarId(idPersonaMedico, "La cuenta médica no es válida.");
        try (ConfiguracionFiscalSifen configuracion =
                     configuracionService.cargarConfiguracion()) {
            if (configuracion.produccion()) {
                throw new IllegalArgumentException(
                        "El envío desde esta pantalla está limitado al ambiente PRUEBA. Producción requiere la homologación y habilitación operativa de la DNIT."
                );
            }

            long identificador;
            DocumentoSifenEnvioDTO documento;
            try (Connection conexion = ConexionBD.obtenerConexion()) {
                try {
                    conexion.setAutoCommit(false);
                    documento = solicitudDAO.buscarParaEnvio(
                                    conexion, idSolicitud, idPersonaMedico
                            )
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "El DE no existe o no pertenece a este profesional."
                            ));
                    if (!"PRUEBA".equals(documento.ambiente())) {
                        throw new IllegalArgumentException(
                                "Solo se pueden transmitir documentos creados en PRUEBA."
                        );
                    }
                    identificador = solicitudDAO.siguienteIdentificadorEnvio(conexion);
                    solicitudDAO.marcarEnviado(conexion, idSolicitud, identificador);
                    conexion.commit();
                } catch (Exception error) {
                    rollback(conexion, error);
                    if (error instanceof IllegalArgumentException validacion) {
                        throw validacion;
                    }
                    if (error instanceof SQLException sql) throw sql;
                    throw new SQLException("No fue posible iniciar el envío SIFEN.", error);
                }
            }

            py.fpune.soracare.sifen.RespuestaRecepcionSifen respuesta;
            char[] password = configuracion.passwordCertificado();
            try {
                respuesta = clienteSoap.enviarPrueba(
                        identificador,
                        documento.xmlFirmado(),
                        configuracion.certificado(),
                        password
                );
            } catch (RuntimeException error) {
                solicitudDAO.guardarErrorTransporte(idSolicitud, error.getMessage());
                throw new IllegalArgumentException(error.getMessage(), error);
            } finally {
                Arrays.fill(password, '\0');
            }
            try (Connection conexion = ConexionBD.obtenerConexion()) {
                return solicitudDAO.guardarRespuesta(
                        conexion, idSolicitud, respuesta
                );
            }
        }
    }

    private byte[] firmar(
            byte[] xml,
            ConfiguracionFiscalSifen configuracion
    ) {
        char[] password = configuracion.passwordCertificado();
        try {
            var credencial = certificadoService.cargar(
                    configuracion.certificado(), password
            );
            return firmaService.firmar(
                    xml, credencial.clavePrivada(), credencial.certificado()
            );
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private QrSifenService.DocumentoConQr agregarQr(
            byte[] xml,
            ConfiguracionFiscalSifen configuracion
    ) {
        char[] csc = configuracion.csc();
        try {
            return qrService.agregar(
                    xml,
                    configuracion.produccion(),
                    configuracion.identificadorCsc(),
                    csc
            );
        } finally {
            Arrays.fill(csc, '\0');
        }
    }

    private static List<ItemDocumentoSifen> items(
            FacturaParaSifenDTO factura,
            OpcionIva opcion
    ) {
        return factura.detalles().stream()
                .map(detalle -> new ItemDocumentoSifen(
                        "SC-" + detalle.idDetalle(),
                        detalle.descripcion(),
                        detalle.cantidad(),
                        detalle.precioUnitario(),
                        opcion.afectacion(),
                        opcion.proporcion(),
                        opcion.tasa()
                ))
                .toList();
    }

    private static ReceptorSifen receptor(
            FacturaParaSifenDTO factura,
            int tipoContribuyente
    ) {
        String tipo = factura.tipoDocumentoReceptor();
        String numero = factura.numeroDocumentoReceptor() == null
                ? "" : factura.numeroDocumentoReceptor().strip();
        if ("RUC".equals(tipo)) {
            if (tipoContribuyente != 1 && tipoContribuyente != 2) {
                throw new IllegalArgumentException(
                        "Indicá si el receptor con RUC es persona física o jurídica."
                );
            }
            var coincidencia = java.util.regex.Pattern
                    .compile("^([0-9]{3,8})-([0-9])$")
                    .matcher(numero);
            if (!coincidencia.matches()) {
                throw new IllegalArgumentException(
                        "El RUC del receptor debe tener el formato número-DV."
                );
            }
            return new ReceptorSifen(
                    true,
                    tipoContribuyente,
                    coincidencia.group(1),
                    Integer.valueOf(coincidencia.group(2)),
                    0,
                    null,
                    null,
                    factura.nombreReceptor(),
                    factura.correoReceptor(),
                    Long.toString(factura.idPago())
            );
        }
        if ("CI".equals(tipo)) {
            String cedula = numero.replaceAll("[^0-9]", "");
            if (!cedula.matches("[0-9]{3,20}")) {
                throw new IllegalArgumentException("La cédula del receptor no es válida.");
            }
            return receptorNoContribuyente(factura, 1, "Cédula paraguaya", cedula);
        }
        if ("PASAPORTE".equals(tipo) && numero.matches("[A-Za-z0-9]{3,20}")) {
            return receptorNoContribuyente(factura, 2, "Pasaporte", numero);
        }
        throw new IllegalArgumentException(
                "El documento del receptor no puede convertirse al formato SIFEN."
        );
    }

    private static ReceptorSifen receptorNoContribuyente(
            FacturaParaSifenDTO factura,
            int tipoDocumento,
            String descripcion,
            String numero
    ) {
        return new ReceptorSifen(
                false,
                null,
                null,
                null,
                tipoDocumento,
                descripcion,
                numero,
                factura.nombreReceptor(),
                factura.correoReceptor(),
                Long.toString(factura.idPago())
        );
    }

    private static void validarFactura(FacturaParaSifenDTO factura) {
        if (!"PYG".equals(factura.moneda())) {
            throw new IllegalArgumentException(
                    "Esta primera versión fiscal solo admite operaciones en guaraníes."
            );
        }
        if (factura.detalles().isEmpty()) {
            throw new IllegalArgumentException("La factura no posee ítems para SIFEN.");
        }
        BigDecimal totalDetalles = factura.detalles().stream()
                .map(detalle -> detalle.precioUnitario().multiply(detalle.cantidad()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDetalles.compareTo(factura.total()) != 0) {
            throw new IllegalArgumentException(
                    "El total de los ítems no coincide con la factura interna."
            );
        }
    }

    private static LocalDateTime fechaLocal(FacturaParaSifenDTO factura) {
        return factura.fechaEmision()
                .atZoneSameInstant(DisponibilidadService.ZONA_HORARIA)
                .toLocalDateTime();
    }

    private String codigoSeguridad() {
        return "%09d".formatted(aleatorio.nextInt(1, 1_000_000_000));
    }

    private static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(contenido)
            );
        } catch (Exception error) {
            throw new IllegalStateException("No fue posible calcular el hash del XML.", error);
        }
    }

    private static void validarId(long id, String mensaje) {
        if (id <= 0) throw new IllegalArgumentException(mensaje);
    }

    private static void rollback(Connection conexion, Exception original) {
        try {
            conexion.rollback();
        } catch (SQLException rollback) {
            original.addSuppressed(rollback);
        }
    }

    private record OpcionIva(
            AfectacionIvaSifen afectacion,
            BigDecimal proporcion,
            int tasa
    ) {
        private static OpcionIva desde(String valor) {
            return switch (valor == null ? "" : valor.strip()) {
                case "GRAVADO_10" -> new OpcionIva(
                        AfectacionIvaSifen.GRAVADO, BigDecimal.valueOf(100), 10
                );
                case "GRAVADO_5" -> new OpcionIva(
                        AfectacionIvaSifen.GRAVADO, BigDecimal.valueOf(100), 5
                );
                case "EXENTO" -> new OpcionIva(
                        AfectacionIvaSifen.EXENTO, BigDecimal.ZERO, 0
                );
                default -> throw new IllegalArgumentException(
                        "Seleccioná el tratamiento de IVA aplicable al servicio."
                );
            };
        }
    }

    private record DatosPago(int codigo, String descripcion) {
        private static DatosPago desde(int codigo) {
            return switch (codigo) {
                case 1 -> new DatosPago(1, "Efectivo");
                case 5 -> new DatosPago(5, "Transferencia");
                default -> throw new IllegalArgumentException(
                        "Seleccioná una forma de pago válida para SIFEN."
                );
            };
        }
    }
}
