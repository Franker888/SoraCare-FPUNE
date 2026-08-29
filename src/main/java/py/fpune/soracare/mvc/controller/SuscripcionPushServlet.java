package py.fpune.soracare.mvc.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.security.ProteccionCsrf;
import py.fpune.soracare.service.FirebaseWebConfigService;
import py.fpune.soracare.service.SuscripcionPushService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/mis-notificaciones/push")
public class SuscripcionPushServlet extends HttpServlet {

    private SuscripcionPushService suscripcionPushService;
    private FirebaseWebConfigService firebaseConfig;

    @Override
    public void init() {
        suscripcionPushService = new SuscripcionPushService();
        firebaseConfig = new FirebaseWebConfigService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");

        try {
            UsuarioAutenticadoDTO usuario = usuarioAutenticado(request);
            if (!ProteccionCsrf.esValido(
                    request.getSession(false), request.getParameter("tokenCsrf"))) {
                responder(response, HttpServletResponse.SC_FORBIDDEN,
                        false, "La confirmación de seguridad no es válida.");
                return;
            }
            if (!firebaseConfig.estaConfigurado()) {
                responder(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        false, "Firebase todavía no está configurado en el servidor.");
                return;
            }
            String accion = request.getParameter("accion");
            String token = request.getParameter("token");
            if ("desactivar".equals(accion)) {
                suscripcionPushService.desactivar(usuario.getIdUsuario(), token);
                responder(response, HttpServletResponse.SC_OK,
                        true, "Las notificaciones de este dispositivo fueron desactivadas.");
                return;
            }
            if (!"registrar".equals(accion)) {
                throw new IllegalArgumentException("La acción solicitada no es válida.");
            }
            suscripcionPushService.registrar(
                    usuario.getIdUsuario(), token, request.getHeader("User-Agent"));
            responder(response, HttpServletResponse.SC_OK,
                    true, "Este dispositivo quedó preparado para recibir avisos.");
        } catch (IllegalArgumentException error) {
            responder(response, HttpServletResponse.SC_BAD_REQUEST, false, error.getMessage());
        } catch (SQLException error) {
            log("No fue posible actualizar la suscripción push.", error);
            responder(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    false, "No fue posible guardar el dispositivo en este momento.");
        }
    }

    private static UsuarioAutenticadoDTO usuarioAutenticado(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        Object usuario = sesion == null ? null : sesion.getAttribute("usuarioAutenticado");
        if (!(usuario instanceof UsuarioAutenticadoDTO autenticado)) {
            throw new IllegalArgumentException("No existe una sesión de paciente válida.");
        }
        return autenticado;
    }

    private static void responder(
            HttpServletResponse response,
            int estado,
            boolean exito,
            String mensaje
    ) throws IOException {
        response.setStatus(estado);
        response.getWriter().write("{\"exito\":" + exito
                + ",\"mensaje\":\"" + escaparJson(mensaje) + "\"}");
    }

    private static String escaparJson(String valor) {
        return valor == null ? "" : valor
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
