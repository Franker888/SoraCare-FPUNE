package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.ResumenFacturacionMedicoDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.FacturaInternaCsvService;
import py.fpune.soracare.service.FacturaInternaService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/medico/facturas/exportar")
public class FacturaInternaCsvServlet extends HttpServlet {

    private FacturaInternaService facturaService;
    private FacturaInternaCsvService csvService;

    @Override
    public void init() {
        facturaService = new FacturaInternaService();
        csvService = new FacturaInternaCsvService();
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        try {
            UsuarioAutenticadoDTO usuario = usuario(request);
            ResumenFacturacionMedicoDTO resumen =
                    facturaService.consultarResumen(usuario.getIdPersona());
            FacturaInternaCsvService.DocumentoCsv documento =
                    csvService.generar(resumen.pagos());

            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader(
                    "Content-Disposition",
                    "attachment; filename=\"" + documento.nombreArchivo() + "\""
            );
            byte[] contenido = documento.contenido();
            response.setContentLength(contenido.length);
            response.getOutputStream().write(contenido);
        } catch (IllegalArgumentException error) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, error.getMessage());
        } catch (SQLException error) {
            log("No fue posible exportar la facturación interna.", error);
            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "No fue posible exportar la facturación interna."
            );
        }
    }

    private static UsuarioAutenticadoDTO usuario(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        Object valor = sesion == null
                ? null
                : sesion.getAttribute("usuarioAutenticado");
        if (!(valor instanceof UsuarioAutenticadoDTO autenticado)) {
            throw new IllegalArgumentException("No existe una sesión médica válida.");
        }
        return autenticado;
    }
}
