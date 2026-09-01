package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.FacturaInternaCreadaDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.security.ProteccionCsrf;
import py.fpune.soracare.service.FacturaInternaService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/medico/facturas")
public class FacturaInternaMedicoServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/views/medico/facturas.jsp";
    private FacturaInternaService facturaService;

    @Override
    public void init() {
        facturaService = new FacturaInternaService();
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
        try {
            UsuarioAutenticadoDTO usuario = usuario(request);
            request.setAttribute(
                    "pagosConfirmados",
                    facturaService.listarParaMedico(usuario.getIdPersona())
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
            request.setAttribute("pagosConfirmados", List.of());
        } catch (SQLException error) {
            log("No fue posible cargar la facturación interna.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (request.getAttribute("error") == null) {
                request.setAttribute(
                        "error",
                        "No fue posible cargar la facturación interna."
                );
            }
            request.setAttribute("pagosConfirmados", List.of());
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

    private static void configurarSinCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
