package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.EliminacionCuentaService;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;

@WebServlet("/cuenta/eliminar")
public class EliminarCuentaServlet extends HttpServlet {

    private static final String VISTA =
            "/WEB-INF/views/perfil/eliminar-cuenta.jsp";

    private static final String ATRIBUTO_USUARIO = "usuarioAutenticado";
    private static final String ATRIBUTO_TOKEN = "tokenEliminarCuenta";

    private EliminacionCuentaService eliminacionCuentaService;

    @Override
    public void init() {
        eliminacionCuentaService = new EliminacionCuentaService();
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        configurarRespuesta(response);
        UsuarioAutenticadoDTO usuario = obtenerPaciente(request, response);
        if (usuario == null) {
            return;
        }

        prepararVista(request, usuario, null);
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        configurarRespuesta(response);
        request.setCharacterEncoding("UTF-8");

        UsuarioAutenticadoDTO usuario = obtenerPaciente(request, response);
        if (usuario == null) {
            return;
        }

        if (!tokenValido(request)) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "La confirmación de seguridad venció. Volvé a intentarlo."
            );
            return;
        }

        try {
            eliminacionCuentaService.eliminarCuenta(
                    usuario
            );

            HttpSession sesion = request.getSession(false);
            if (sesion != null) {
                sesion.invalidate();
            }

            response.sendRedirect(
                    request.getContextPath() + "/login?cuenta=eliminada"
            );
        } catch (IllegalArgumentException | IllegalStateException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            prepararVista(request, usuario, error.getMessage());
            request.getRequestDispatcher(VISTA).forward(request, response);
        } catch (SecurityException error) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (SQLException error) {
            log("No fue posible inactivar la cuenta del paciente.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            prepararVista(
                    request,
                    usuario,
                    "No fue posible eliminar la cuenta en este momento. "
                            + "Intentá nuevamente."
            );
            request.getRequestDispatcher(VISTA).forward(request, response);
        }
    }

    private static UsuarioAutenticadoDTO obtenerPaciente(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        HttpSession sesion = request.getSession(false);
        if (sesion == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }

        Object atributo = sesion.getAttribute(ATRIBUTO_USUARIO);
        if (!(atributo instanceof UsuarioAutenticadoDTO usuario)) {
            sesion.invalidate();
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }

        if (!usuario.tieneRol("PACIENTE") || usuario.getRoles().size() != 1) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        return usuario;
    }

    private static void prepararVista(
            HttpServletRequest request,
            UsuarioAutenticadoDTO usuario,
            String error
    ) {
        HttpSession sesion = request.getSession(true);
        String token = crearToken();
        sesion.setAttribute(ATRIBUTO_TOKEN, token);
        request.setAttribute("tokenEliminarCuenta", token);
        request.setAttribute("usuario", usuario);
        request.setAttribute("error", error);
    }

    private static boolean tokenValido(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null) {
            return false;
        }

        Object guardado = sesion.getAttribute(ATRIBUTO_TOKEN);
        String recibido = request.getParameter("token");
        if (!(guardado instanceof String esperado) || recibido == null) {
            return false;
        }

        sesion.removeAttribute(ATRIBUTO_TOKEN);
        return MessageDigest.isEqual(
                esperado.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                recibido.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }

    private static String crearToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static void configurarRespuesta(HttpServletResponse response) {
        response.setHeader(
                "Cache-Control",
                "no-store, no-cache, must-revalidate, max-age=0"
        );
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
