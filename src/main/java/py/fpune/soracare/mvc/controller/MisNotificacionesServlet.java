package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.security.ProteccionCsrf;
import py.fpune.soracare.service.FirebaseWebConfigService;
import py.fpune.soracare.service.NotificacionService;
import py.fpune.soracare.service.SuscripcionPushService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/mis-notificaciones")
public class MisNotificacionesServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/views/paciente/mis-notificaciones.jsp";
    private NotificacionService notificacionService;
    private FirebaseWebConfigService firebaseConfig;
    private SuscripcionPushService suscripcionPushService;

    @Override
    public void init() {
        notificacionService = new NotificacionService();
        firebaseConfig = new FirebaseWebConfigService();
        suscripcionPushService = new SuscripcionPushService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        if ("leida".equals(request.getParameter("resultado"))) {
            request.setAttribute("mensajeExito", "La notificación fue marcada como leída.");
        } else if ("todas-leidas".equals(request.getParameter("resultado"))) {
            request.setAttribute("mensajeExito", "Todas las notificaciones quedaron como leídas.");
        }
        mostrarVista(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        configurarSinCache(response);
        try {
            UsuarioAutenticadoDTO usuario = usuarioAutenticado(request);
            if (!ProteccionCsrf.esValido(
                    request.getSession(false), request.getParameter("tokenCsrf"))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "La confirmación de seguridad no es válida.");
                return;
            }
            if ("todas".equals(request.getParameter("accion"))) {
                notificacionService.marcarTodasLeidas(usuario.getIdPersona());
                response.sendRedirect(request.getContextPath()
                        + "/mis-notificaciones?resultado=todas-leidas");
                return;
            }
            long idNotificacion = convertirId(request.getParameter("idNotificacion"));
            notificacionService.marcarLeida(usuario.getIdPersona(), idNotificacion);
            response.sendRedirect(request.getContextPath()
                    + "/mis-notificaciones?resultado=leida");
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            mostrarVista(request, response);
        } catch (SQLException error) {
            log("Error al actualizar notificaciones.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("error", "No fue posible actualizar las notificaciones.");
            mostrarVista(request, response);
        }
    }

    private void mostrarVista(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            UsuarioAutenticadoDTO usuario = usuarioAutenticado(request);
            request.setAttribute("tokenCsrf",
                    ProteccionCsrf.obtenerOCrear(request.getSession(false)));
            request.setAttribute("notificaciones",
                    notificacionService.listarParaPaciente(usuario.getIdPersona()));
            configurarFirebase(request, usuario);
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            request.setAttribute("error", error.getMessage());
            request.setAttribute("notificaciones", List.of());
            request.setAttribute("firebaseConfigurado", false);
            request.setAttribute("suscripcionesPushActivas", 0);
        } catch (SQLException error) {
            log("Error al cargar notificaciones.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (request.getAttribute("error") == null) {
                request.setAttribute("error", "No fue posible cargar las notificaciones.");
            }
            request.setAttribute("notificaciones", List.of());
            request.setAttribute("firebaseConfigurado", false);
            request.setAttribute("suscripcionesPushActivas", 0);
        }
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    private void configurarFirebase(
            HttpServletRequest request,
            UsuarioAutenticadoDTO usuario
    ) {
        boolean configurado = firebaseConfig.estaConfigurado();
        request.setAttribute("firebaseConfigurado", configurado);
        request.setAttribute("firebaseApiKey", firebaseConfig.apiKey());
        request.setAttribute("firebaseAuthDomain", firebaseConfig.authDomain());
        request.setAttribute("firebaseProjectId", firebaseConfig.projectId());
        request.setAttribute("firebaseStorageBucket", firebaseConfig.storageBucket());
        request.setAttribute("firebaseMessagingSenderId", firebaseConfig.messagingSenderId());
        request.setAttribute("firebaseAppId", firebaseConfig.appId());
        request.setAttribute("firebaseVapidKey", firebaseConfig.vapidKey());
        request.setAttribute("suscripcionesPushActivas", 0);
        if (!configurado) {
            return;
        }
        try {
            request.setAttribute("suscripcionesPushActivas",
                    suscripcionPushService.contarActivas(usuario.getIdUsuario()));
        } catch (SQLException error) {
            log("No fue posible contar las suscripciones push activas.", error);
            request.setAttribute("firebaseAdvertencia",
                    "Los avisos del navegador no están disponibles temporalmente.");
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

    private static long convertirId(String valor) {
        try {
            long id = Long.parseLong(valor);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("La notificación seleccionada no es válida.", error);
        }
    }

    private static void configurarSinCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
