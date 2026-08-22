package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.ConfiguracionSistemaService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/admin/configuracion")
public class ConfiguracionAdministrativaServlet extends HttpServlet {

    private final ConfiguracionSistemaService configuracionService =
            new ConfiguracionSistemaService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        cargarVista(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        UsuarioAutenticadoDTO usuario = obtenerUsuario(request);
        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        try {
            configuracionService.actualizar(
                    request.getParameter("idConfiguracion"),
                    request.getParameter("valor"),
                    request.getParameter("motivo"),
                    usuario.getIdUsuario()
            );
            response.sendRedirect(request.getContextPath()
                    + "/admin/configuracion?resultado=actualizada");
        } catch (IllegalArgumentException error) {
            request.setAttribute("error", error.getMessage());
            cargarVista(request, response);
        } catch (SQLException error) {
            log("No fue posible actualizar la configuración.", error);
            request.setAttribute("error", "No fue posible guardar el cambio.");
            cargarVista(request, response);
        }
    }

    private void cargarVista(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("configuraciones", configuracionService.listar());
            request.setAttribute("historial", configuracionService.listarHistorial());
        } catch (SQLException error) {
            log("No fue posible cargar la configuración.", error);
            request.setAttribute("error", "No fue posible cargar las configuraciones.");
        }
        request.getRequestDispatcher("/WEB-INF/views/admin/configuracion.jsp")
                .forward(request, response);
    }

    private static UsuarioAutenticadoDTO obtenerUsuario(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null) return null;
        Object valor = sesion.getAttribute("usuarioAutenticado");
        return valor instanceof UsuarioAutenticadoDTO usuario ? usuario : null;
    }
}
