package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.CredencialesAutenticacionDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.util.UrlPublicaRequest;
import py.fpune.soracare.service.AutenticacionService;
import py.fpune.soracare.service.ConfiguracionGoogleService;
import py.fpune.soracare.service.TurnstileService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final String VISTA_LOGIN =
            "/WEB-INF/views/autenticacion/login.jsp";

    private static final String ATRIBUTO_USUARIO_AUTENTICADO =
            "usuarioAutenticado";

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

    private static final String RUTA_CAMBIO_CONTRASENA =
            "/cambiar-contrasena";

    private static final int DURACION_SESION_SEGUNDOS =
            30 * 60;

    private AutenticacionService autenticacionService;
    private ConfiguracionGoogleService configuracionGoogleService;
    private TurnstileService turnstileService;

    @Override
    public void init() {
        autenticacionService =
                new AutenticacionService();
        configuracionGoogleService =
                new ConfiguracionGoogleService();
        turnstileService = new TurnstileService();
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

        if (sesionExistente != null) {

            Object usuarioEnSesion =
                    sesionExistente.getAttribute(
                            ATRIBUTO_USUARIO_AUTENTICADO
                    );

            if (usuarioEnSesion
                    instanceof UsuarioAutenticadoDTO usuarioAutenticado) {

                redirigirSegunEstadoContrasena(
                        request,
                        response,
                        usuarioAutenticado
                );

                return;
            }
        }

        prepararIntegracionesPublicas(request);
        recuperarErrorGoogle(request);

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
            turnstileService.validar(
                    request.getParameter("cf-turnstile-response"),
                    "login"
            );

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

            redirigirSegunEstadoContrasena(
                    request,
                    response,
                    usuarioAutenticado
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

        } catch (IOException error) {

            log(
                    "No fue posible validar Turnstile durante "
                            + "el inicio de sesión.",
                    error
            );

            mostrarError(
                    request,
                    response,
                    "No pudimos completar la verificación de seguridad. "
                            + "Intentá nuevamente en unos instantes."
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

    private static void redirigirSegunEstadoContrasena(
            HttpServletRequest request,
            HttpServletResponse response,
            UsuarioAutenticadoDTO usuarioAutenticado
    ) throws IOException {

        String rutaDestino;
        if (usuarioAutenticado.isDebeCambiarContrasena()) {
            rutaDestino = RUTA_CAMBIO_CONTRASENA;
        } else if (usuarioAutenticado.tieneRol("ADMINISTRADOR")) {
            rutaDestino = RUTA_ADMINISTRACION;
        } else if (usuarioAutenticado.tieneRol("RECEPCIONISTA")) {
            rutaDestino = RUTA_RECEPCION;
        } else if (usuarioAutenticado.tieneRol("MEDICO")) {
            rutaDestino = RUTA_MEDICO;
        } else if (usuarioAutenticado.tieneRol("PACIENTE")) {
            rutaDestino = RUTA_PACIENTE;
        } else {
            rutaDestino = RUTA_DASHBOARD;
        }

        response.sendRedirect(
                request.getContextPath()
                        + rutaDestino
        );
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

        prepararIntegracionesPublicas(request);

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
                "Referrer-Policy",
                "no-referrer-when-downgrade"
        );

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

    private void prepararAccesoGoogle(HttpServletRequest request) {
        String clientId = configuracionGoogleService.obtenerClientId();
        if (clientId == null) {
            return;
        }
        request.setAttribute("googleClientId", clientId);
        request.setAttribute(
                "googleLoginUri",
                UrlPublicaRequest.construir(request, "/auth/google")
        );
    }

    private void prepararIntegracionesPublicas(HttpServletRequest request) {
        prepararAccesoGoogle(request);
        String siteKey = turnstileService.obtenerSiteKey();
        if (siteKey != null) {
            request.setAttribute("turnstileSiteKey", siteKey);
        }
    }

    private static void recuperarErrorGoogle(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null) {
            return;
        }
        Object error = sesion.getAttribute(
                AutenticacionGoogleServlet.ATRIBUTO_ERROR_GOOGLE
        );
        if (error instanceof String mensaje && !mensaje.isBlank()) {
            request.setAttribute("error", mensaje);
            sesion.removeAttribute(
                    AutenticacionGoogleServlet.ATRIBUTO_ERROR_GOOGLE
            );
        }
    }
}
