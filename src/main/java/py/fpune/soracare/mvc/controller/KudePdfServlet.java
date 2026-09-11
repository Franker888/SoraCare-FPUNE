package py.fpune.soracare.mvc.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.KudeService;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/facturas/kude")
public class KudePdfServlet extends HttpServlet {
    private KudeService service;
    public KudePdfServlet() { }
    KudePdfServlet(KudeService service) { this.service = service; }
    @Override public void init() { service = new KudeService(); }

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setHeader("Cache-Control", "private, no-store, max-age=0");
        response.setHeader("X-Content-Type-Options", "nosniff");
        var sesion = request.getSession(false);
        Object cuenta = sesion == null ? null : sesion.getAttribute("usuarioAutenticado");
        if (!(cuenta instanceof UsuarioAutenticadoDTO usuario)) {
            response.sendError(401); return;
        }
        try {
            long id = Long.parseLong(request.getParameter("id"));
            var pdf = service.obtener(id, usuario);
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=\"" + pdf.nombreArchivo() + "\"");
            response.setContentLength(pdf.contenido().length);
            response.getOutputStream().write(pdf.contenido());
        } catch (IllegalArgumentException error) {
            response.sendError(403, "KuDE no disponible para esta cuenta o estado del documento.");
        } catch (SQLException | IllegalStateException error) {
            log("No fue posible generar el KuDE.", error);
            response.sendError(500, "No fue posible generar el KuDE.");
        }
    }
}
