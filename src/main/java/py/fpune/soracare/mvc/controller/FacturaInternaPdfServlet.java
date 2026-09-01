package py.fpune.soracare.mvc.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.FacturaInternaArchivoDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.FacturaInternaService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/facturas/interna")
public class FacturaInternaPdfServlet extends HttpServlet {

    private FacturaInternaService facturaService;

    @Override
    public void init() {
        facturaService = new FacturaInternaService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setHeader("Cache-Control", "private, no-store, max-age=0");
        try {
            FacturaInternaArchivoDTO archivo = facturaService.obtenerArchivo(
                    convertirId(request.getParameter("id")),
                    usuario(request)
            );
            response.setContentType("application/pdf");
            response.setContentLength(archivo.contenidoPdf().length);
            response.setHeader(
                    "Content-Disposition",
                    "inline; filename=\""
                            + archivo.nombreArchivo().replace("\"", "") + "\""
            );
            response.getOutputStream().write(archivo.contenidoPdf());
        } catch (IllegalArgumentException error) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, error.getMessage());
        } catch (SQLException error) {
            log("No fue posible descargar la factura interna.", error);
            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "No fue posible descargar la factura interna."
            );
        }
    }

    private static UsuarioAutenticadoDTO usuario(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        Object usuario = sesion == null
                ? null
                : sesion.getAttribute("usuarioAutenticado");
        return usuario instanceof UsuarioAutenticadoDTO autenticado
                ? autenticado
                : null;
    }

    private static long convertirId(String valor) {
        try {
            long id = Long.parseLong(valor == null ? "" : valor.strip());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    "La factura interna seleccionada no es válida.",
                    error
            );
        }
    }
}
