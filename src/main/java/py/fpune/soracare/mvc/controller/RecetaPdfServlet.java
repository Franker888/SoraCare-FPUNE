package py.fpune.soracare.mvc.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.RecetaMedicaDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.RecetaMedicaService;
import py.fpune.soracare.service.RecetaPdfService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

@WebServlet(urlPatterns = {"/mis-recetas/pdf", "/medico/recetas/pdf"})
public class RecetaPdfServlet extends HttpServlet {

    private RecetaMedicaService recetaMedicaService;
    private RecetaPdfService recetaPdfService;

    @Override
    public void init() {
        recetaMedicaService = new RecetaMedicaService();
        recetaPdfService = new RecetaPdfService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        configurarSinCache(response);
        try {
            long idReceta = convertirId(request.getParameter("id"));
            UsuarioAutenticadoDTO usuario = usuarioAutenticado(request);
            boolean rutaMedico = request.getServletPath().startsWith("/medico/");
            RecetaMedicaDTO receta = rutaMedico
                    ? recetaMedicaService.buscarParaMedico(idReceta, usuario.getIdPersona())
                    : recetaMedicaService.buscarParaPaciente(idReceta, usuario.getIdPersona());
            RecetaPdfService.DocumentoPdf pdf = recetaPdfService.generar(receta);
            String disposicion = "1".equals(request.getParameter("descargar"))
                    ? "attachment" : "inline";
            String nombre = URLEncoder.encode(
                    pdf.nombreArchivo(), StandardCharsets.UTF_8).replace("+", "%20");
            response.setContentType("application/pdf");
            response.setContentLength(pdf.contenido().length);
            response.setHeader("Content-Disposition",
                    disposicion + "; filename*=UTF-8''" + nombre);
            response.getOutputStream().write(pdf.contenido());
        } catch (IllegalArgumentException error) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, error.getMessage());
        } catch (SQLException error) {
            log("Error al abrir el PDF de una indicación médica.", error);
            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "No fue posible generar la indicación médica.");
        }
    }

    private static UsuarioAutenticadoDTO usuarioAutenticado(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        Object usuario = sesion == null ? null : sesion.getAttribute("usuarioAutenticado");
        if (!(usuario instanceof UsuarioAutenticadoDTO autenticado)) {
            throw new IllegalArgumentException("No existe una sesión válida.");
        }
        return autenticado;
    }

    private static long convertirId(String valor) {
        try {
            long id = Long.parseLong(valor);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("La indicación médica seleccionada no es válida.", error);
        }
    }

    private static void configurarSinCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
