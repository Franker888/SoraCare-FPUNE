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
import py.fpune.soracare.service.VerificacionContrasenaException;

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

    private static final String RUTA_ADMINISTRACION =
            "/admin/inicio";

    private static final String RUTA_RECEPCION =
            "/recepcion/inicio";

    private static final String RUTA_MEDICO =
            "/medico/inicio";

    private static final String RUTA_PACIENTE =
            "/paciente/inicio";

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

        prepararVista(
                request,
                usuarioAutenticado
        );

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

        prepararVista(
                request,
                usuarioAutenticado
        );

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
                                    usuarioAutenticado,
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

        } catch (VerificacionContrasenaException error) {

            log(
                    "No fue posible consultar Pwned Passwords durante "
                            + "el cambio de contraseña.",
                    error
            );

            mostrarError(
                    request,
                    response,
                    error.getMessage(),
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE
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

        UsuarioAutenticadoDTO usuarioAutenticado =
                obtenerUsuarioAutenticado(request);
        if (usuarioAutenticado != null) {
            prepararVista(request, usuarioAutenticado);
        }

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

        UsuarioAutenticadoDTO usuario = obtenerUsuarioAutenticado(request);
        String ruta = RUTA_DASHBOARD;
        if (usuario != null && usuario.tieneRol("ADMINISTRADOR")) {
            ruta = RUTA_ADMINISTRACION;
        } else if (usuario != null && usuario.tieneRol("RECEPCIONISTA")) {
            ruta = RUTA_RECEPCION;
        } else if (usuario != null && usuario.tieneRol("MEDICO")) {
            ruta = RUTA_MEDICO;
        } else if (usuario != null && usuario.tieneRol("PACIENTE")) {
            ruta = RUTA_PACIENTE;
        }

        response.sendRedirect(
                request.getContextPath()
                        + ruta
        );
    }

    private static void prepararVista(
            HttpServletRequest request,
            UsuarioAutenticadoDTO usuario
    ) {
        request.setAttribute(
                "cambioObligatorio",
                usuario.isDebeCambiarContrasena()
        );
        request.setAttribute(
                "rutaVolver",
                rutaPanel(usuario)
        );
    }

    private static String rutaPanel(UsuarioAutenticadoDTO usuario) {
        if (usuario.tieneRol("ADMINISTRADOR")) {
            return RUTA_ADMINISTRACION;
        }
        if (usuario.tieneRol("RECEPCIONISTA")) {
            return RUTA_RECEPCION;
        }
        if (usuario.tieneRol("MEDICO")) {
            return RUTA_MEDICO;
        }
        if (usuario.tieneRol("PACIENTE")) {
            return RUTA_PACIENTE;
        }
        return RUTA_DASHBOARD;
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
