package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.fpune.soracare.service.AdministracionSeguridadService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/admin/roles")
public class RolesAdministrativosServlet extends HttpServlet {

    private final AdministracionSeguridadService seguridadService =
            new AdministracionSeguridadService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("roles", seguridadService.listarRoles());
        } catch (SQLException error) {
            log("No fue posible listar los roles.", error);
            request.setAttribute("error", "No fue posible cargar los roles del sistema.");
        }
        request.getRequestDispatcher("/WEB-INF/views/admin/roles.jsp")
                .forward(request, response);
    }
}
