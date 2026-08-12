package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.AgendaMedicoDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.AgendaMedicoService;
import py.fpune.soracare.service.DisponibilidadService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@WebServlet("/medico/agenda")
public class AgendaMedicoServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/views/medico/agenda.jsp";

    private AgendaMedicoService agendaMedicoService;

    @Override
    public void init() {
        agendaMedicoService = new AgendaMedicoService();
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        configurarSinCache(response);
        LocalDate fecha = convertirFecha(request.getParameter("fecha"));

        try {
            UsuarioAutenticadoDTO usuario = usuarioAutenticado(request);
            AgendaMedicoDTO agenda = agendaMedicoService.consultar(
                    usuario.getIdPersona(),
                    fecha
            );
            request.setAttribute("agenda", agenda);
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            request.setAttribute("error", error.getMessage());
        } catch (SQLException error) {
            log("Error al cargar la agenda del médico.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute(
                    "error",
                    "No fue posible cargar tu agenda médica. Intentá nuevamente."
            );
        }

        request.setAttribute("fecha", fecha);
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    private static UsuarioAutenticadoDTO usuarioAutenticado(
            HttpServletRequest request
    ) {
        HttpSession sesion = request.getSession(false);
        Object usuario = sesion == null
                ? null
                : sesion.getAttribute("usuarioAutenticado");
        if (!(usuario instanceof UsuarioAutenticadoDTO autenticado)) {
            throw new IllegalArgumentException("No existe una sesión médica válida.");
        }
        return autenticado;
    }

    private static LocalDate convertirFecha(String valor) {
        try {
            return valor == null || valor.isBlank()
                    ? LocalDate.now(DisponibilidadService.ZONA_HORARIA)
                    : LocalDate.parse(valor.strip());
        } catch (DateTimeParseException error) {
            return LocalDate.now(DisponibilidadService.ZONA_HORARIA);
        }
    }

    private static void configurarSinCache(HttpServletResponse response) {
        response.setHeader(
                "Cache-Control",
                "no-store, no-cache, must-revalidate, max-age=0"
        );
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
