package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.RecetaMedicaService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/mis-recetas")
public class MisRecetasServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/views/paciente/mis-recetas.jsp";
    private RecetaMedicaService recetaMedicaService;

    @Override
    public void init() {
        recetaMedicaService = new RecetaMedicaService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        configurarSinCache(response);
        try {
            UsuarioAutenticadoDTO usuario = usuarioAutenticado(request);
            request.setAttribute(
                    "recetas", recetaMedicaService.listarParaPaciente(usuario.getIdPersona()));
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            request.setAttribute("error", error.getMessage());
            request.setAttribute("recetas", List.of());
        } catch (SQLException error) {
            log("Error al cargar las indicaciones médicas del paciente.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("error", "No fue posible cargar tus indicaciones médicas.");
            request.setAttribute("recetas", List.of());
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

    private static void configurarSinCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
