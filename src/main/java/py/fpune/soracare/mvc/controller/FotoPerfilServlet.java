package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.PerfilVisualService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/perfil/foto")
public class FotoPerfilServlet extends HttpServlet {

    private PerfilVisualService perfilService;

    @Override
    public void init() {
        perfilService = new PerfilVisualService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarRespuesta(response);
        HttpSession sesion = request.getSession(false);
        Object valor = sesion == null ? null : sesion.getAttribute("usuarioAutenticado");
        if (!(valor instanceof UsuarioAutenticadoDTO usuario)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            PerfilVisualService.FotoDescargable foto =
                    perfilService.abrirFoto(usuario.getIdPersona());
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(foto.tipoMime());
            response.setContentLength(foto.contenido().length);
            response.setHeader("Content-Disposition", "inline; filename=perfil");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.getOutputStream().write(foto.contenido());
        } catch (IllegalArgumentException error) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, error.getMessage());
        } catch (SQLException | IOException error) {
            log("No fue posible abrir la fotografía de perfil.", error);
            if (!response.isCommitted()) {
                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "No fue posible abrir la fotografía de perfil."
                );
            }
        }
    }

    private static void configurarRespuesta(HttpServletResponse response) {
        response.setHeader("Cache-Control", "private, no-store, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
