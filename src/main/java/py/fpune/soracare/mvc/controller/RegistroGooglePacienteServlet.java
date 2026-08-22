package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.PerfilGoogleDTO;
import py.fpune.soracare.dto.RegistroCuentaPacienteDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.AutenticacionGoogleService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/google/registro")
public class RegistroGooglePacienteServlet extends HttpServlet {

    private static final String VISTA =
            "/WEB-INF/views/autenticacion/registro-google.jsp";

    private AutenticacionGoogleService autenticacionGoogleService;

    @Override
    public void init() {
        autenticacionGoogleService = new AutenticacionGoogleService();
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        PerfilGoogleDTO perfil = obtenerPerfil(request);
        if (perfil == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        request.setAttribute("perfilGoogle", perfil);
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        PerfilGoogleDTO perfil = obtenerPerfil(request);
        if (perfil == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        RegistroCuentaPacienteDTO datos = new RegistroCuentaPacienteDTO(
                request.getParameter("nombres"),
                request.getParameter("apellidos"),
                request.getParameter("cedula"),
                request.getParameter("fechaNacimiento"),
                request.getParameter("telefono"),
                perfil.correo(),
                request.getParameter("direccion"),
                request.getParameter("grupoSanguineo"),
                request.getParameter("contactoEmergencia"),
                request.getParameter("telefonoEmergencia"),
                request.getParameter("nombreUsuario"),
                null,
                null
        );

        try {
            UsuarioAutenticadoDTO usuario =
                    autenticacionGoogleService.registrarPaciente(
                            perfil,
                            datos
                    );
            AutenticacionGoogleServlet.iniciarSesion(request, usuario);
            response.sendRedirect(
                    request.getContextPath() + "/paciente/inicio"
            );
        } catch (IllegalArgumentException | SecurityException error) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", error.getMessage());
            request.setAttribute("perfilGoogle", perfil);
            request.getRequestDispatcher(VISTA).forward(request, response);
        } catch (SQLException error) {
            log("No fue posible registrar al paciente con Google.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute(
                    "error",
                    "No fue posible completar el registro. Revisá los datos."
            );
            request.setAttribute("perfilGoogle", perfil);
            request.getRequestDispatcher(VISTA).forward(request, response);
        }
    }

    private static PerfilGoogleDTO obtenerPerfil(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null) {
            return null;
        }
        Object valor = sesion.getAttribute(
                AutenticacionGoogleServlet.ATRIBUTO_PERFIL_PENDIENTE
        );
        return valor instanceof PerfilGoogleDTO perfil ? perfil : null;
    }
}
