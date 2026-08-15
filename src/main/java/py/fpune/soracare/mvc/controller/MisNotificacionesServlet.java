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
import java.util.List;

@WebServlet("/mis-notificaciones")
public class MisNotificacionesServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/views/paciente/mis-notificaciones.jsp";
    private NotificacionService notificacionService;

    @Override
    public void init() {
        notificacionService = new NotificacionService();
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
            request.setAttribute("notificaciones",
                    notificacionService.listarParaPaciente(usuario.getIdPersona()));
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            request.setAttribute("error", error.getMessage());
            request.setAttribute("notificaciones", List.of());
        } catch (SQLException error) {
            log("Error al cargar notificaciones.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (request.getAttribute("error") == null) {
                request.setAttribute("error", "No fue posible cargar las notificaciones.");
            }
            request.setAttribute("notificaciones", List.of());
        }
        request.getRequestDispatcher(VISTA).forward(request, response);
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
