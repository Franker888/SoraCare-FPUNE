package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.PerfilGoogleDTO;
import py.fpune.soracare.dto.ResultadoInicioGoogleDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.AutenticacionGoogleService;
import py.fpune.soracare.service.TokenGoogleService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.sql.SQLException;

@WebServlet("/auth/google")
public class AutenticacionGoogleServlet extends HttpServlet {

    public static final String ATRIBUTO_PERFIL_PENDIENTE =
            "perfilGooglePendiente";
    public static final String ATRIBUTO_USUARIO_PENDIENTE =
            "idUsuarioGooglePendiente";
    public static final String ATRIBUTO_ERROR_GOOGLE =
            "errorGoogle";
    public static final String ATRIBUTO_GOOGLE_LOGIN_HINT =
            "googleLoginHint";

    private static final int DURACION_SESION_SEGUNDOS = 30 * 60;

    private TokenGoogleService tokenGoogleService;
    private AutenticacionGoogleService autenticacionGoogleService;

    @Override
    public void init() {
        tokenGoogleService = new TokenGoogleService();
        autenticacionGoogleService = new AutenticacionGoogleService();
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        configurarSinCache(response);
        request.setCharacterEncoding("UTF-8");

        try {
            validarTokenCsrf(request);
            PerfilGoogleDTO perfil = tokenGoogleService.verificar(
                    request.getParameter("credential")
            );
            ResultadoInicioGoogleDTO resultado =
                    autenticacionGoogleService.iniciar(perfil);

            switch (resultado.estado()) {
                case AUTENTICADO -> {
                    iniciarSesion(
                            request,
                            resultado.usuarioAutenticado(),
                            perfil
                    );
                    response.sendRedirect(
                            request.getContextPath() + "/paciente/inicio"
                    );
                }
                case REQUIERE_VINCULACION -> {
                    HttpSession sesion = prepararSesionPendiente(
                            request,
                            perfil
                    );
                    sesion.setAttribute(
                            ATRIBUTO_USUARIO_PENDIENTE,
                            resultado.idUsuarioExistente()
                    );
                    response.sendRedirect(
                            request.getContextPath() + "/google/vincular"
                    );
                }
                case REQUIERE_REGISTRO -> {
                    prepararSesionPendiente(request, perfil);
                    response.sendRedirect(
                            request.getContextPath() + "/google/registro"
                    );
                }
            }
        } catch (SecurityException | IllegalArgumentException error) {
            redirigirConError(request, response, error.getMessage());
        } catch (GeneralSecurityException | IOException error) {
            log("No fue posible validar el token de Google.", error);
            redirigirConError(
                    request,
                    response,
                    "Google no pudo confirmar tu identidad. Intentá nuevamente."
            );
        } catch (SQLException error) {
            log("Error de base de datos durante el acceso con Google.", error);
            redirigirConError(
                    request,
                    response,
                    "No fue posible procesar el acceso con Google en este momento."
            );
        }
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        response.sendRedirect(request.getContextPath() + "/login");
    }

    private static void validarTokenCsrf(HttpServletRequest request) {
        String csrfFormulario = request.getParameter("g_csrf_token");
        String csrfCookie = obtenerCookie(request, "g_csrf_token");

        if (csrfFormulario == null
                || csrfCookie == null
                || !MessageDigest.isEqual(
                csrfFormulario.getBytes(StandardCharsets.UTF_8),
                csrfCookie.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new SecurityException(
                    "La solicitud de Google no superó la validación de seguridad."
            );
        }
    }

    private static String obtenerCookie(
            HttpServletRequest request,
            String nombre
    ) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (nombre.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static HttpSession prepararSesionPendiente(
            HttpServletRequest request,
            PerfilGoogleDTO perfil
    ) {
        HttpSession sesionAnterior = request.getSession(false);
        if (sesionAnterior != null) {
            sesionAnterior.invalidate();
        }
        HttpSession sesion = request.getSession(true);
        sesion.setMaxInactiveInterval(10 * 60);
        sesion.setAttribute(ATRIBUTO_PERFIL_PENDIENTE, perfil);
        return sesion;
    }

    public static void iniciarSesion(
            HttpServletRequest request,
            UsuarioAutenticadoDTO usuario
    ) {
        HttpSession anterior = request.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        HttpSession nueva = request.getSession(true);
        nueva.setMaxInactiveInterval(DURACION_SESION_SEGUNDOS);
        nueva.setAttribute("usuarioAutenticado", usuario);
    }

    public static void iniciarSesion(
            HttpServletRequest request,
            UsuarioAutenticadoDTO usuario,
            PerfilGoogleDTO perfil
    ) {
        iniciarSesion(request, usuario);
        if (perfil != null
                && perfil.correo() != null
                && !perfil.correo().isBlank()) {
            request.getSession(false).setAttribute(
                    ATRIBUTO_GOOGLE_LOGIN_HINT,
                    perfil.correo().strip()
            );
        }
    }

    private static void redirigirConError(
            HttpServletRequest request,
            HttpServletResponse response,
            String mensaje
    ) throws IOException {
        HttpSession sesion = request.getSession(true);
        sesion.setAttribute(ATRIBUTO_ERROR_GOOGLE, mensaje);
        response.sendRedirect(request.getContextPath() + "/login");
    }

    private static void configurarSinCache(HttpServletResponse response) {
        response.setHeader(
                "Cache-Control",
                "no-store, no-cache, must-revalidate, max-age=0"
        );
        response.setHeader("Pragma", "no-cache");
    }
}
