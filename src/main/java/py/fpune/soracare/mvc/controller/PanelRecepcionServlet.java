package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.RecepcionService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/recepcion/inicio")
public class PanelRecepcionServlet extends HttpServlet {

    private static final String VISTA_PANEL_RECEPCION =
            "/WEB-INF/views/recepcion/inicio.jsp";

    private static final String ATRIBUTO_USUARIO_AUTENTICADO =
            "usuarioAutenticado";

    private RecepcionService recepcionService;

    @Override
    public void init() {
        recepcionService = new RecepcionService();
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        configurarRespuestaSinCache(response);

        HttpSession sesion = request.getSession(false);
        Object usuarioEnSesion = sesion == null
                ? null
                : sesion.getAttribute(ATRIBUTO_USUARIO_AUTENTICADO);

        if (!(usuarioEnSesion instanceof UsuarioAutenticadoDTO usuarioAutenticado)) {
            if (sesion != null) {
                sesion.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        request.setAttribute(ATRIBUTO_USUARIO_AUTENTICADO, usuarioAutenticado);

        try {
            request.setAttribute(
                    "resumenRecepcion",
                    recepcionService.consultarResumen()
            );
        } catch (SQLException error) {
            log("No fue posible resumir el panel de recepción.", error);
            request.setAttribute("errorResumen", true);
        }

        request.getRequestDispatcher(VISTA_PANEL_RECEPCION).forward(request, response);
    }

    private static void configurarRespuestaSinCache(HttpServletResponse response) {
        response.setHeader(
                "Cache-Control",
                "no-store, no-cache, must-revalidate, max-age=0"
        );
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
