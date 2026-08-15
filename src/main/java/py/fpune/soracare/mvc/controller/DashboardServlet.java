package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.NotificacionService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    private static final String VISTA_DASHBOARD =
            "/WEB-INF/views/dashboard/dashboard.jsp";

    private static final String ATRIBUTO_USUARIO_AUTENTICADO =
            "usuarioAutenticado";

    private NotificacionService notificacionService;

    @Override
    public void init() {
        notificacionService = new NotificacionService();
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        configurarRespuestaSinCache(
                response
        );

        HttpSession sesion =
                request.getSession(false);

        if (sesion == null) {
            redirigirAlLogin(
                    request,
                    response
            );

            return;
        }

        Object atributoUsuario =
                sesion.getAttribute(
                        ATRIBUTO_USUARIO_AUTENTICADO
                );

        if (!(atributoUsuario
                instanceof UsuarioAutenticadoDTO usuarioAutenticado)) {

            sesion.invalidate();

            redirigirAlLogin(
                    request,
                    response
            );

            return;
        }

        request.setAttribute(
                ATRIBUTO_USUARIO_AUTENTICADO,
                usuarioAutenticado
        );

        if (usuarioAutenticado.tieneRol("PACIENTE")) {
            try {
                request.setAttribute(
                        "cantidadNotificacionesNoLeidas",
                        notificacionService.contarNoLeidasParaPaciente(
                                usuarioAutenticado.getIdPersona()
                        )
                );
            } catch (SQLException | IllegalArgumentException error) {
                log("No fue posible contar las notificaciones del paciente.", error);
                request.setAttribute("cantidadNotificacionesNoLeidas", 0L);
            }
        }

        request.getRequestDispatcher(
                VISTA_DASHBOARD
        ).forward(
                request,
                response
        );
    }

    private static void redirigirAlLogin(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.sendRedirect(
                request.getContextPath()
                        + "/login"
        );
    }

    private static void configurarRespuestaSinCache(
            HttpServletResponse response
    ) {
        response.setHeader(
                "Cache-Control",
                "no-store, no-cache, "
                        + "must-revalidate, max-age=0"
        );

        response.setHeader(
                "Pragma",
                "no-cache"
        );

        response.setDateHeader(
                "Expires",
                0
        );
    }
}
