package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.SolicitudCambioContrasenaDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.CambioContrasenaService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/cambiar-contrasena")
public class CambioContrasenaServlet extends HttpServlet {

    private static final String VISTA_CAMBIO_CONTRASENA =
            "/WEB-INF/views/autenticacion/cambiar-contrasena.jsp";

    private static final String ATRIBUTO_USUARIO_AUTENTICADO =
            "usuarioAutenticado";

    private static final String RUTA_LOGIN =
            "/login";

    private static final String RUTA_DASHBOARD =
            "/dashboard";

    private CambioContrasenaService cambioContrasenaService;

    @Override
    public void init() {
        cambioContrasenaService =
                new CambioContrasenaService();
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        configurarRespuestaSinCache(
                response
        );

        UsuarioAutenticadoDTO usuarioAutenticado =
                obtenerUsuarioAutenticado(
                        request
                );

        if (usuarioAutenticado == null) {
            redirigirAlLogin(
                    request,
                    response
            );

            return;
        }

        if (!usuarioAutenticado
                .isDebeCambiarContrasena()) {

            redirigirAlDashboard(
                    request,
                    response
            );

            return;
        }

        request.getRequestDispatcher(
                VISTA_CAMBIO_CONTRASENA
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

        UsuarioAutenticadoDTO usuarioAutenticado =
                obtenerUsuarioAutenticado(
                        request
                );

        if (usuarioAutenticado == null) {
            redirigirAlLogin(
                    request,
                    response
            );

            return;
        }

        if (!usuarioAutenticado
                .isDebeCambiarContrasena()) {

            redirigirAlDashboard(
                    request,
                    response
            );

            return;
        }

        String contrasenaActual =
                request.getParameter(
                        "contrasenaActual"
                );

        String nuevaContrasena =
                request.getParameter(
                        "nuevaContrasena"
                );

        String confirmacionNuevaContrasena =
                request.getParameter(
                        "confirmacionNuevaContrasena"
                );

        try {
            SolicitudCambioContrasenaDTO solicitud =
                    new SolicitudCambioContrasenaDTO(
                            contrasenaActual,
                            nuevaContrasena,
                            confirmacionNuevaContrasena
                    );

            UsuarioAutenticadoDTO usuarioActualizado =
                    cambioContrasenaService
                            .cambiarContrasena(
                                    usuarioAutenticado
                                            .getIdUsuario(),
                                    solicitud
                            );

            HttpSession sesion =
                    request.getSession(false);

            sesion.setAttribute(
                    ATRIBUTO_USUARIO_AUTENTICADO,
                    usuarioActualizado
            );

            sesion.setAttribute(
                    "mensajeExito",
                    "La contraseña fue actualizada correctamente."
            );

            redirigirAlDashboard(
                    request,
                    response
            );

        } catch (IllegalArgumentException error) {

            mostrarError(
                    request,
                    response,
                    error.getMessage(),
                    HttpServletResponse.SC_BAD_REQUEST
            );

        } catch (SecurityException error) {

            mostrarError(
                    request,
                    response,
                    error.getMessage(),
                    HttpServletResponse.SC_UNAUTHORIZED
            );

        } catch (SQLException error) {

            log(
                    "Error de base de datos durante "
                            + "el cambio de contraseña.",
                    error
            );

            mostrarError(
                    request,
                    response,
                    "No fue posible actualizar la contraseña. "
                            + "Intentá nuevamente.",
                    HttpServletResponse
                            .SC_INTERNAL_SERVER_ERROR
            );
        }
    }

    private static UsuarioAutenticadoDTO obtenerUsuarioAutenticado(
            HttpServletRequest request
    ) {
        HttpSession sesion =
                request.getSession(false);

        if (sesion == null) {
            return null;
        }

        Object usuarioEnSesion =
                sesion.getAttribute(
                        ATRIBUTO_USUARIO_AUTENTICADO
                );

        if (usuarioEnSesion
                instanceof UsuarioAutenticadoDTO usuarioAutenticado) {

            return usuarioAutenticado;
        }

        return null;
    }

    private void mostrarError(
            HttpServletRequest request,
            HttpServletResponse response,
            String mensaje,
            int estadoHttp
    ) throws ServletException, IOException {

        request.setAttribute(
                "error",
                mensaje
        );

        response.setStatus(
                estadoHttp
        );

        request.getRequestDispatcher(
                VISTA_CAMBIO_CONTRASENA
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
                        + RUTA_LOGIN
        );
    }

    private static void redirigirAlDashboard(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.sendRedirect(
                request.getContextPath()
                        + RUTA_DASHBOARD
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