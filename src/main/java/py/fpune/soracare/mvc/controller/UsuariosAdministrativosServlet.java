package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.AdministracionSeguridadService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/admin/usuarios")
public class UsuariosAdministrativosServlet extends HttpServlet {

    private final AdministracionSeguridadService seguridadService =
            new AdministracionSeguridadService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        cargarVista(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        UsuarioAutenticadoDTO administrador = obtenerUsuario(request);
        if (administrador == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        try {
            String accion = request.getParameter("accion");
            if ("desbloquear".equals(accion)) {
                seguridadService.desbloquear(request.getParameter("idUsuario"));
            } else if ("estado".equals(accion)) {
                seguridadService.actualizarEstado(
                        request.getParameter("idUsuario"),
                        request.getParameter("nuevoEstado"),
                        administrador.getIdUsuario()
                );
            } else {
                throw new IllegalArgumentException("La acción seleccionada no es válida.");
            }
            response.sendRedirect(request.getContextPath() + "/admin/usuarios?resultado=actualizado");
        } catch (IllegalArgumentException error) {
            request.setAttribute("error", error.getMessage());
            cargarVista(request, response);
        } catch (SQLException error) {
            log("No fue posible actualizar el usuario.", error);
            request.setAttribute("error", "No fue posible actualizar la cuenta.");
            cargarVista(request, response);
        }
    }

    private void cargarVista(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("resumen", seguridadService.listarUsuarios(
                    request.getParameter("q"), request.getParameter("estado")));
        } catch (IllegalArgumentException error) {
            request.setAttribute("error", error.getMessage());
        } catch (SQLException error) {
            log("No fue posible listar los usuarios.", error);
            request.setAttribute("error", "No fue posible cargar las cuentas.");
        }
        request.getRequestDispatcher("/WEB-INF/views/admin/usuarios.jsp")
                .forward(request, response);
    }

    private static UsuarioAutenticadoDTO obtenerUsuario(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null) return null;
        Object valor = sesion.getAttribute("usuarioAutenticado");
        return valor instanceof UsuarioAutenticadoDTO usuario ? usuario : null;
    }
}
