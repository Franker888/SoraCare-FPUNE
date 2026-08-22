package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.service.EstadoSistemaService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/admin/estado-sistema")
public class EstadoSistemaAdministrativoServlet extends HttpServlet {

    private final EstadoSistemaService estadoService = new EstadoSistemaService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("estado", estadoService.consultar());
        } catch (SQLException error) {
            log("No fue posible consultar el estado del sistema.", error);
            request.setAttribute("error", "PostgreSQL no respondió correctamente.");
        }
        request.getRequestDispatcher("/WEB-INF/views/admin/estado-sistema.jsp")
                .forward(request, response);
    }
}
