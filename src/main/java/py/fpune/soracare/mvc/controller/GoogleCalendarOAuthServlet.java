package py.fpune.soracare.mvc.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.AutorizacionGoogleCalendarDTO;
import py.fpune.soracare.dto.PerfilGoogleDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.security.ProteccionCsrf;
import py.fpune.soracare.service.GoogleCalendarService;
import py.fpune.soracare.service.GoogleOAuthService;
import py.fpune.soracare.service.TokenGoogleService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@WebServlet(urlPatterns = {
        "/google/calendar/conectar",
        "/google/calendar/callback"
})
public class GoogleCalendarOAuthServlet extends HttpServlet {

    public static final String ATRIBUTO_MENSAJE_CALENDAR =
            "mensajeGoogleCalendar";
    public static final String ATRIBUTO_ERROR_CALENDAR =
            "errorGoogleCalendar";

    private static final String ATRIBUTO_ESTADO =
            "googleCalendarOAuthState";
    private static final String ATRIBUTO_ESTADO_CREADO =
            "googleCalendarOAuthStateCreado";
    private static final Duration DURACION_ESTADO = Duration.ofMinutes(10);
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private GoogleOAuthService oauth;
    private GoogleCalendarService calendar;
    private TokenGoogleService tokenGoogle;

    @Override
    public void init() {
        oauth = new GoogleOAuthService();
        calendar = new GoogleCalendarService();
        tokenGoogle = new TokenGoogleService();
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        if (!request.getServletPath().endsWith("/conectar")) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        HttpSession sesion = request.getSession(false);
        requerirPaciente(sesion);
        if (!ProteccionCsrf.esValido(
                sesion,
                request.getParameter("csrfToken")
        )) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "La solicitud no superó la validación de seguridad."
            );
            return;
        }
        if (!calendar.estaDisponible()) {
            guardarError(
                    sesion,
                    "Google Calendar todavía requiere completar la configuración segura del servidor."
            );
            redirigirCitas(request, response);
            return;
        }

        String estado = nuevoEstado();
        sesion.setAttribute(ATRIBUTO_ESTADO, estado);
        sesion.setAttribute(ATRIBUTO_ESTADO_CREADO, Instant.now());
        Object pista = sesion.getAttribute(
                AutenticacionGoogleServlet.ATRIBUTO_GOOGLE_LOGIN_HINT
        );
        String loginHint = pista instanceof String correo
                ? correo
                : null;
        response.sendRedirect(
                oauth.crearUrlAutorizacion(estado, loginHint).toString()
        );
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        if (!request.getServletPath().endsWith("/callback")) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        HttpSession sesion = request.getSession(false);
        UsuarioAutenticadoDTO usuario = requerirPaciente(sesion);
        try {
            validarEstado(sesion, request.getParameter("state"));
            if (request.getParameter("error") != null) {
                throw new SecurityException(
                        "La autorización de Google Calendar fue cancelada."
                );
            }

            AutorizacionGoogleCalendarDTO autorizacion =
                    oauth.intercambiarCodigo(
                            request.getParameter("code")
                    );
            PerfilGoogleDTO perfil = tokenGoogle.verificar(
                    autorizacion.idToken()
            );
            calendar.vincular(
                    usuario.getIdUsuario(),
                    perfil,
                    autorizacion
            );
            sesion.setAttribute(
                    AutenticacionGoogleServlet.ATRIBUTO_GOOGLE_LOGIN_HINT,
                    perfil.correo()
            );
            sesion.setAttribute(
                    ATRIBUTO_MENSAJE_CALENDAR,
                    "Google Calendar quedó vinculado. Desde ahora las citas se guardan sin volver a conectar la cuenta."
            );
        } catch (SecurityException | IllegalArgumentException
                 | IllegalStateException error) {
            guardarError(sesion, error.getMessage());
        } catch (GeneralSecurityException | IOException error) {
            log("No fue posible validar la autorización de Google.", error);
            guardarError(
                    sesion,
                    "Google no pudo confirmar la autorización de Calendar. Intentá nuevamente."
            );
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            guardarError(
                    sesion,
                    "La conexión con Google fue interrumpida. Intentá nuevamente."
            );
        } catch (SQLException error) {
            log("No fue posible guardar la vinculación de Calendar.", error);
            guardarError(
                    sesion,
                    "No fue posible guardar la vinculación con Google Calendar."
            );
        } finally {
            limpiarEstado(sesion);
        }
        redirigirCitas(request, response);
    }

    private static UsuarioAutenticadoDTO requerirPaciente(
            HttpSession sesion
    ) {
        Object valor = sesion == null
                ? null
                : sesion.getAttribute("usuarioAutenticado");
        if (!(valor instanceof UsuarioAutenticadoDTO usuario)
                || !usuario.tieneRol("PACIENTE")) {
            throw new SecurityException(
                    "Iniciá sesión como paciente para vincular Google Calendar."
            );
        }
        return usuario;
    }

    private static void validarEstado(
            HttpSession sesion,
            String recibido
    ) {
        Object esperado = sesion.getAttribute(ATRIBUTO_ESTADO);
        Object creado = sesion.getAttribute(ATRIBUTO_ESTADO_CREADO);
        boolean coincide = esperado instanceof String valor
                && recibido != null
                && MessageDigest.isEqual(
                valor.getBytes(StandardCharsets.UTF_8),
                recibido.getBytes(StandardCharsets.UTF_8)
        );
        boolean vigente = creado instanceof Instant instante
                && Duration.between(instante, Instant.now())
                .compareTo(DURACION_ESTADO) <= 0;
        if (!coincide || !vigente) {
            throw new SecurityException(
                    "La autorización de Google venció o no superó la validación de seguridad."
            );
        }
    }

    private static String nuevoEstado() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private static void limpiarEstado(HttpSession sesion) {
        sesion.removeAttribute(ATRIBUTO_ESTADO);
        sesion.removeAttribute(ATRIBUTO_ESTADO_CREADO);
    }

    private static void guardarError(HttpSession sesion, String mensaje) {
        sesion.setAttribute(
                ATRIBUTO_ERROR_CALENDAR,
                mensaje == null || mensaje.isBlank()
                        ? "No fue posible vincular Google Calendar."
                        : mensaje
        );
    }

    private static void redirigirCitas(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        response.sendRedirect(request.getContextPath() + "/mis-citas");
    }
}
