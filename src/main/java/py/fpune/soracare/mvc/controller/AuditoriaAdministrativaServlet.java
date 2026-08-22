package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.dto.FiltroAuditoriaDTO;
import py.fpune.soracare.service.AuditoriaService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/admin/auditoria")
public class AuditoriaAdministrativaServlet extends HttpServlet {

    private final AuditoriaService auditoriaService = new AuditoriaService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            FiltroAuditoriaDTO filtro = auditoriaService.crearFiltro(
                    request.getParameter("desde"),
                    request.getParameter("hasta"),
                    request.getParameter("tipo"),
                    request.getParameter("modulo"),
                    request.getParameter("resultado"),
                    request.getParameter("q")
            );
            request.setAttribute("filtro", filtro);
            request.setAttribute("resumen", auditoriaService.consultar(filtro));
        } catch (IllegalArgumentException error) {
            request.setAttribute("error", error.getMessage());
        } catch (SQLException error) {
            log("No fue posible consultar la auditoría.", error);
            request.setAttribute("error", "No fue posible consultar la auditoría en este momento.");
        }

        request.getRequestDispatcher("/WEB-INF/views/admin/auditoria.jsp")
                .forward(request, response);
    }
}
