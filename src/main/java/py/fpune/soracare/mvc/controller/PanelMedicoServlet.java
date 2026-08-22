package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.PerfilVisualDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.PanelMedicoService;
import py.fpune.soracare.service.PerfilVisualService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/medico/inicio")
public class PanelMedicoServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/views/medico/inicio.jsp";

    private PanelMedicoService panelMedicoService;
    private PerfilVisualService perfilVisualService;

    @Override
    public void init() {
        panelMedicoService = new PanelMedicoService();
        perfilVisualService = new PerfilVisualService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        UsuarioAutenticadoDTO usuario = usuarioAutenticado(request);
        request.setAttribute("usuarioAutenticado", usuario);

        try {
            request.setAttribute(
                    "resumenMedico",
                    panelMedicoService.consultar(usuario.getIdPersona())
            );
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            request.setAttribute("error", error.getMessage());
        } catch (SQLException error) {
            log("No fue posible cargar el panel médico.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute(
                    "error",
                    "No fue posible cargar el resumen médico. Intentá nuevamente."
            );
        }

        try {
            request.setAttribute(
                    "perfilVisual",
                    perfilVisualService.consultar(usuario.getIdPersona())
            );
        } catch (SQLException error) {
            log("No fue posible cargar la fotografía del perfil médico.", error);
            request.setAttribute("perfilVisual", new PerfilVisualDTO("AZUL", false));
        }

        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    private static UsuarioAutenticadoDTO usuarioAutenticado(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        Object usuario = sesion == null ? null : sesion.getAttribute("usuarioAutenticado");
        if (!(usuario instanceof UsuarioAutenticadoDTO autenticado)) {
            throw new IllegalArgumentException("No existe una sesión médica válida.");
        }
        return autenticado;
    }

    private static void configurarSinCache(HttpServletResponse response) {
        response.setHeader(
                "Cache-Control",
                "no-store, no-cache, must-revalidate, max-age=0"
        );
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
