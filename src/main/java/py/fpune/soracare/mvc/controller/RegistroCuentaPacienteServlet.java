package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.RegistroCuentaPacienteDTO;
import py.fpune.soracare.service.RegistroCuentaPacienteService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/registro")
public class RegistroCuentaPacienteServlet extends HttpServlet {

    private RegistroCuentaPacienteService registroService;

    @Override
    public void init() {
        registroService = new RegistroCuentaPacienteService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (estaAutenticado(request)) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }
        response.setHeader("Cache-Control", "no-store, max-age=0");
        request.getRequestDispatcher("/WEB-INF/views/autenticacion/registro-paciente.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (estaAutenticado(request)) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        RegistroCuentaPacienteDTO datos = new RegistroCuentaPacienteDTO(
                request.getParameter("nombres"), request.getParameter("apellidos"),
                request.getParameter("cedula"), request.getParameter("fechaNacimiento"),
                request.getParameter("telefono"), request.getParameter("correo"),
                request.getParameter("direccion"), request.getParameter("grupoSanguineo"),
                request.getParameter("contactoEmergencia"), request.getParameter("telefonoEmergencia"),
                request.getParameter("nombreUsuario"), request.getParameter("contrasena"),
                request.getParameter("confirmacionContrasena"));
        try {
            registroService.registrar(datos);
            response.sendRedirect(request.getContextPath() + "/login?registro=exitoso");
        } catch (IllegalArgumentException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/autenticacion/registro-paciente.jsp")
                    .forward(request, response);
        } catch (SQLException error) {
            log("Error al crear una cuenta de paciente.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("error", "No fue posible crear la cuenta. Revisá los datos e intentá nuevamente.");
            request.getRequestDispatcher("/WEB-INF/views/autenticacion/registro-paciente.jsp")
                    .forward(request, response);
        }
    }

    private static boolean estaAutenticado(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        return sesion != null && sesion.getAttribute("usuarioAutenticado") != null;
    }
}
