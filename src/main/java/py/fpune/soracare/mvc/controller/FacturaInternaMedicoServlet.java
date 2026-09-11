package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.FacturaInternaCreadaDTO;
import py.fpune.soracare.dto.ResumenFacturacionMedicoDTO;
import py.fpune.soracare.dto.SolicitudSifenResumenDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.security.ProteccionCsrf;
import py.fpune.soracare.service.ConfiguracionSifenService;
import py.fpune.soracare.service.FacturaInternaService;
import py.fpune.soracare.service.IntegracionSifenService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

@WebServlet("/medico/facturas")
public class FacturaInternaMedicoServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/views/medico/facturas.jsp";
    private FacturaInternaService facturaService;
    private ConfiguracionSifenService configuracionSifenService;
    private IntegracionSifenService integracionSifenService;
    private py.fpune.soracare.service.ConsultaSifenService consultaSifenService;

    @Override
    public void init() {
        facturaService = new FacturaInternaService();
        configuracionSifenService = new ConfiguracionSifenService();
        integracionSifenService = new IntegracionSifenService();
        consultaSifenService = new py.fpune.soracare.service.ConsultaSifenService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        if ("emitida".equals(request.getParameter("resultado"))) {
            request.setAttribute(
                    "mensajeExito",
                    "La factura interna fue emitida y ya está disponible para el paciente."
            );
        } else if ("sifen-validado".equals(request.getParameter("resultado"))) {
            request.setAttribute(
                    "mensajeExito",
                    "El DE fue generado, firmado y validado localmente contra el XSD oficial. Todavía no fue enviado ni aprobado por la DNIT."
            );
        } else if ("sifen-respondido".equals(request.getParameter("resultado"))) {
            request.setAttribute(
                    "mensajeExito",
                    "SIFEN respondió el envío de prueba. Revisá el estado fiscal y el mensaje registrado en la factura."
            );
        }
        mostrarVista(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        request.setCharacterEncoding("UTF-8");
        HttpSession sesion = request.getSession(false);
        if (!ProteccionCsrf.esValido(sesion, request.getParameter("csrfToken"))) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "La solicitud de facturación no es válida."
            );
            return;
        }

        try {
            UsuarioAutenticadoDTO usuario = usuario(request);
            if ("consultarSifenPrueba".equals(request.getParameter("accion"))) {
                request.setAttribute("consultaSifen", consultaSifenService.consultar(
                        convertirIdSolicitud(request.getParameter("idSolicitud")), usuario));
                mostrarVista(request, response);
            } else if ("enviarSifenPrueba".equals(request.getParameter("accion"))) {
                SolicitudSifenResumenDTO solicitud = integracionSifenService
                        .enviarAPruebas(
                                convertirIdSolicitud(
                                        request.getParameter("idSolicitud")
                                ),
                                usuario.getIdPersona()
                        );
                response.sendRedirect(
                        request.getContextPath()
                                + "/medico/facturas?resultado=sifen-respondido&idSifen="
                                + solicitud.idSolicitud()
                );
            } else if ("prepararSifen".equals(request.getParameter("accion"))) {
                SolicitudSifenResumenDTO solicitud = integracionSifenService
                        .prepararYFirmar(
                                convertirIdFactura(request.getParameter("idFactura")),
                                usuario.getIdPersona(),
                                request.getParameter("tratamientoIva"),
                                convertirEntero(request.getParameter("tipoPagoSifen")),
                                convertirEnteroOpcional(
                                        request.getParameter("tipoContribuyenteReceptor"), 1
                                )
                        );
                response.sendRedirect(
                        request.getContextPath()
                                + "/medico/facturas?resultado=sifen-validado&idSifen="
                                + solicitud.idSolicitud()
                );
            } else {
                FacturaInternaCreadaDTO factura = facturaService.emitir(
                        convertirId(request.getParameter("idPago")),
                        usuario.getIdPersona(),
                        usuario.getIdUsuario()
                );
                response.sendRedirect(
                        request.getContextPath()
                                + "/medico/facturas?resultado=emitida&id="
                                + factura.idFactura()
                );
            }
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            mostrarVista(request, response);
        } catch (SQLException error) {
            log("No fue posible emitir la factura interna.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute(
                    "error",
                    "No fue posible emitir la factura interna. Intentá nuevamente."
            );
            mostrarVista(request, response);
        }
    }

    private void mostrarVista(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute(
                "estadoSifen",
                configuracionSifenService.consultarEstado()
        );
        try {
            UsuarioAutenticadoDTO usuario = usuario(request);
            request.setAttribute(
                    "resumenFacturacion",
                    facturaService.consultarResumen(usuario.getIdPersona())
            );
            request.setAttribute(
                    "solicitudesSifen",
                    integracionSifenService.listarParaMedico(usuario.getIdPersona())
            );
            request.setAttribute(
                    "csrfToken",
                    ProteccionCsrf.obtenerOCrear(request.getSession(false))
            );
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            if (request.getAttribute("error") == null) {
                request.setAttribute("error", error.getMessage());
            }
            request.setAttribute(
                    "resumenFacturacion",
                    ResumenFacturacionMedicoDTO.vacio()
            );
            request.setAttribute("solicitudesSifen", Map.of());
        } catch (SQLException error) {
            log("No fue posible cargar la facturación interna.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (request.getAttribute("error") == null) {
                request.setAttribute(
                        "error",
                        "No fue posible cargar la facturación interna."
                );
            }
            request.setAttribute(
                    "resumenFacturacion",
                    ResumenFacturacionMedicoDTO.vacio()
            );
            request.setAttribute("solicitudesSifen", Map.of());
        }
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    private static UsuarioAutenticadoDTO usuario(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        Object usuario = sesion == null
                ? null
                : sesion.getAttribute("usuarioAutenticado");
        if (!(usuario instanceof UsuarioAutenticadoDTO autenticado)) {
            throw new IllegalArgumentException("No existe una sesión médica válida.");
        }
        return autenticado;
    }

    private static long convertirId(String valor) {
        try {
            long id = Long.parseLong(valor == null ? "" : valor.strip());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("El pago seleccionado no es válido.", error);
        }
    }

    private static long convertirIdFactura(String valor) {
        try {
            long id = Long.parseLong(valor == null ? "" : valor.strip());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    "La factura interna seleccionada no es válida.", error
            );
        }
    }

    private static long convertirIdSolicitud(String valor) {
        try {
            long id = Long.parseLong(valor == null ? "" : valor.strip());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    "El documento SIFEN seleccionado no es válido.", error
            );
        }
    }

    private static int convertirEntero(String valor) {
        try {
            return Integer.parseInt(valor == null ? "" : valor.strip());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("La opción fiscal no es válida.", error);
        }
    }

    private static int convertirEnteroOpcional(String valor, int predeterminado) {
        return valor == null || valor.isBlank()
                ? predeterminado
                : convertirEntero(valor);
    }

    private static void configurarSinCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
