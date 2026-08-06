package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.CredencialesAutenticacionDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.AutenticacionService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final String VISTA_LOGIN =
            "/WEB-INF/views/autenticacion/login.jsp";

    private static final String ATRIBUTO_USUARIO_AUTENTICADO =
            "usuarioAutenticado";

    private static final int DURACION_SESION_SEGUNDOS =
            30 * 60;

    private AutenticacionService autenticacionService;

    @Override
    public void init() {
        autenticacionService =
                new AutenticacionService();
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        configurarRespuestaSinCache(
                response
        );

        HttpSession sesionExistente =
                request.getSession(false);

        if (sesionExistente != null
                && sesionExistente.getAttribute(
                ATRIBUTO_USUARIO_AUTENTICADO
        ) != null) {

            response.sendRedirect(
                    request.getContextPath()
                            + "/dashboard"
            );

            return;
        }

        request.getRequestDispatcher(
                VISTA_LOGIN
        ).forward(
                request,
                response
        );
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        configurarRespuestaSinCache(
                response
        );

        request.setCharacterEncoding(
                "UTF-8"
        );

        String identificador =
                obtenerParametro(
                        request,
                        "identificador"
                );

        String contrasena =
                request.getParameter(
                        "contrasena"
                );

        request.setAttribute(
                "identificador",
                identificador
        );

        try {
            CredencialesAutenticacionDTO credenciales =
                    new CredencialesAutenticacionDTO(
                            identificador,
                            contrasena
                    );

            UsuarioAutenticadoDTO usuarioAutenticado =
                    autenticacionService.autenticar(
                            credenciales
                    );

            reemplazarSesionAnterior(
                    request
            );

            HttpSession nuevaSesion =
                    request.getSession(true);

            nuevaSesion.setMaxInactiveInterval(
                    DURACION_SESION_SEGUNDOS
            );

            nuevaSesion.setAttribute(
                    ATRIBUTO_USUARIO_AUTENTICADO,
                    usuarioAutenticado
            );

            response.sendRedirect(
                    request.getContextPath()
                            + "/dashboard"
            );

        } catch (IllegalArgumentException error) {

            mostrarError(
                    request,
                    response,
                    error.getMessage()
            );

        } catch (SecurityException error) {

            mostrarError(
                    request,
                    response,
                    error.getMessage()
            );

        } catch (SQLException error) {

            log(
                    "Error de base de datos durante "
                            + "el inicio de sesión.",
                    error
            );

            mostrarError(
                    request,
                    response,
                    "No fue posible procesar el inicio de sesión. "
                            + "Intentá nuevamente."
            );
        }
    }

    private void mostrarError(
            HttpServletRequest request,
            HttpServletResponse response,
            String mensaje
    ) throws ServletException, IOException {

        request.setAttribute(
                "error",
                mensaje
        );

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        request.getRequestDispatcher(
                VISTA_LOGIN
        ).forward(
                request,
                response
        );
    }

    private static String obtenerParametro(
            HttpServletRequest request,
            String nombre
    ) {
        String valor =
                request.getParameter(
                        nombre
                );

        return valor == null
                ? ""
                : valor.strip();
    }

    private static void reemplazarSesionAnterior(
            HttpServletRequest request
    ) {
        HttpSession sesionAnterior =
                request.getSession(false);

        if (sesionAnterior != null) {
            sesionAnterior.invalidate();
        }
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