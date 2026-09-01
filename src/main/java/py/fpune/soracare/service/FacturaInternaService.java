package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.FacturaInternaDAO;
import py.fpune.soracare.dao.NotificacionDAO;
import py.fpune.soracare.dao.PagoDAO;
import py.fpune.soracare.dto.DatosFacturacionDTO;
import py.fpune.soracare.dto.FacturaInternaArchivoDTO;
import py.fpune.soracare.dto.FacturaInternaCreadaDTO;
import py.fpune.soracare.dto.PagoResumenDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;

import java.sql.Connection;
import java.sql.SQLException;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FacturaInternaService {

    private static final System.Logger LOGGER =
            System.getLogger(FacturaInternaService.class.getName());

    private final PagoDAO pagoDAO = new PagoDAO();
    private final FacturaInternaDAO facturaDAO = new FacturaInternaDAO();
    private final NotificacionDAO notificacionDAO = new NotificacionDAO();
    private final MedicoService medicoService = new MedicoService();
    private final FacturaInternaPdfService pdfService = new FacturaInternaPdfService();
    private final CorreoResendService correoService = new CorreoResendService();

    public List<PagoResumenDTO> listarParaMedico(long idPersonaMedico)
            throws SQLException {
        validarId(idPersonaMedico, "La cuenta médica no es válida.");
        medicoService.buscarPorIdPersona(idPersonaMedico)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tu cuenta no posee una ficha médica asociada."
                ));
        return pagoDAO.listarConfirmadosPorMedicoPersona(idPersonaMedico);
    }

    public FacturaInternaCreadaDTO emitir(
            long idPago,
            long idPersonaMedico,
            long idUsuario
    ) throws SQLException {
        validarId(idPago, "El pago seleccionado no es válido.");
        validarId(idPersonaMedico, "La cuenta médica no es válida.");
        validarId(idUsuario, "El usuario emisor no es válido.");

        FacturaInternaCreadaDTO factura;
        PagoResumenDTO pago;
        DatosFacturacionDTO facturacion;
        ComprobantePdfService.DocumentoPdf documento;

        try (Connection conexion = ConexionBD.obtenerConexion()) {
            try {
                conexion.setAutoCommit(false);
                pagoDAO.bloquear(conexion, idPago);
                pago = pagoDAO.buscarPorId(conexion, idPago)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "El pago seleccionado no existe."
                        ));
                if (!facturaDAO.perteneceAlMedico(
                        conexion, idPago, idPersonaMedico)) {
                    throw new IllegalArgumentException(
                            "No podés emitir documentos de una consulta de otro profesional."
                    );
                }
                if (!pago.estaConfirmado()) {
                    throw new IllegalArgumentException(
                            "La factura interna solo puede emitirse para un pago confirmado."
                    );
                }

                FacturaInternaCreadaDTO existente = facturaDAO
                        .buscarPorPago(conexion, idPago)
                        .orElse(null);
                if (existente != null) {
                    conexion.commit();
                    return existente;
                }

                facturacion = pagoDAO.buscarDatosFacturacion(conexion, idPago);
                factura = facturaDAO.crear(
                        conexion, pago, facturacion, idUsuario
                );
                documento = pdfService.generar(
                        pago, facturacion, factura.numeroFactura()
                );
                facturaDAO.guardarDocumento(
                        conexion,
                        factura.idFactura(),
                        documento.nombreArchivo(),
                        documento.contenido(),
                        documento.sha256()
                );
                notificacionDAO.registrarFacturaInternaEmitida(
                        conexion,
                        pago.idCita(),
                        pago.idPago(),
                        factura.numeroFactura()
                );
                conexion.commit();
            } catch (Exception error) {
                rollback(conexion, error);
                if (error instanceof IllegalArgumentException validacion) {
                    throw validacion;
                }
                if (error instanceof SQLException sql) throw sql;
                throw new SQLException("No fue posible emitir la factura interna.", error);
            }
        }

        programarEnvio(factura, pago, facturacion, documento);
        return factura;
    }

    public FacturaInternaArchivoDTO obtenerArchivo(
            long idFactura,
            UsuarioAutenticadoDTO usuario
    ) throws SQLException {
        validarId(idFactura, "La factura interna seleccionada no es válida.");
        if (usuario == null) {
            throw new IllegalArgumentException("No existe una sesión válida.");
        }
        FacturaInternaArchivoDTO archivo = facturaDAO.buscarArchivo(idFactura)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La factura interna seleccionada no existe."
                ));
        boolean personalAdministrativo = usuario.tieneRol("ADMINISTRADOR")
                || usuario.tieneRol("RECEPCIONISTA");
        boolean medicoPropietario = usuario.tieneRol("MEDICO")
                && usuario.getIdPersona() == archivo.idPersonaMedico();
        boolean pacientePropietario = usuario.tieneRol("PACIENTE")
                && usuario.getIdPersona() == archivo.idPersonaPaciente();
        if (!personalAdministrativo && !medicoPropietario && !pacientePropietario) {
            throw new IllegalArgumentException(
                    "No tenés permiso para descargar esta factura interna."
            );
        }
        if (archivo.contenidoPdf() == null || archivo.contenidoPdf().length == 0) {
            throw new IllegalArgumentException(
                    "La factura interna todavía no posee un archivo disponible."
            );
        }
        return archivo;
    }

    private void programarEnvio(
            FacturaInternaCreadaDTO factura,
            PagoResumenDTO pago,
            DatosFacturacionDTO facturacion,
            ComprobantePdfService.DocumentoPdf documento
    ) {
        if (!correoService.estaConfigurado()) {
            actualizarEstadoCorreo(factura.idFactura(), "NO_CONFIGURADO");
            return;
        }
        DespachadorCorreoService.ejecutar(() -> {
            try {
                correoService.enviar(
                        facturacion.correo(),
                        "Factura interna " + factura.numeroFactura()
                                + " - SoraCare-FPUNE",
                        contenidoCorreo(pago, factura.numeroFactura()),
                        List.of(new CorreoResendService.AdjuntoCorreo(
                                documento.nombreArchivo(), documento.contenido()
                        )),
                        "factura-interna-" + factura.idFactura() + "-v1"
                );
                actualizarEstadoCorreo(factura.idFactura(), "ENVIADO");
            } catch (Exception error) {
                LOGGER.log(
                        System.Logger.Level.ERROR,
                        "No fue posible enviar la factura interna {0} por correo.",
                        factura.numeroFactura()
                );
                actualizarEstadoCorreo(factura.idFactura(), "FALLIDO");
            }
        });
    }

    private void actualizarEstadoCorreo(long idFactura, String estado) {
        try {
            facturaDAO.registrarResultadoCorreo(idFactura, estado);
        } catch (SQLException error) {
            LOGGER.log(
                    System.Logger.Level.ERROR,
                    "No fue posible guardar el estado del correo de la factura {0}.",
                    idFactura
            );
        }
    }

    private static String contenidoCorreo(PagoResumenDTO pago, String numero) {
        String fecha = pago.fechaCita()
                .atZoneSameInstant(DisponibilidadService.ZONA_HORARIA)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        return """
                <!doctype html>
                <html lang="es"><body style="font-family:Arial,sans-serif;color:#10213a">
                  <div style="max-width:620px;margin:24px auto;padding:28px;border:1px solid #dbe5f1;border-radius:16px">
                    <h1 style="color:#2452c7">Factura interna disponible</h1>
                    <p>Hola, <strong>%s</strong>.</p>
                    <p>Adjuntamos la factura interna <strong>%s</strong> de tu consulta de %s con %s, prevista para el %s.</p>
                    <p><strong>Total: Gs. %s</strong></p>
                    <p style="padding:14px;background:#fff4d6;border-radius:10px;color:#6b4b00">
                      Documento comercial interno sin validez tributaria. No contiene timbrado, CDC ni aprobación de e-Kuatia/SIFEN.
                    </p>
                  </div>
                </body></html>
                """.formatted(
                escaparHtml(pago.nombrePaciente()),
                escaparHtml(numero),
                escaparHtml(pago.nombreEspecialidad()),
                escaparHtml(pago.nombreMedico()),
                escaparHtml(fecha),
                pago.monto().setScale(0, RoundingMode.HALF_UP).toPlainString()
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
}
