package py.fpune.soracare.mvc.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.PerfilGoogleDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.service.AutenticacionGoogleService;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/google/vincular")
public class VincularGoogleServlet extends HttpServlet {

    private static final String VISTA =
            "/WEB-INF/views/autenticacion/vincular-google.jsp";

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
        if (perfil == null || obtenerIdUsuario(request) == null) {
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
        Long idUsuario = obtenerIdUsuario(request);
        if (perfil == null || idUsuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            UsuarioAutenticadoDTO usuario =
                    autenticacionGoogleService.vincularCuentaExistente(
                            perfil,
                            idUsuario,
                            request.getParameter("contrasena")
                    );
            AutenticacionGoogleServlet.iniciarSesion(request, usuario, perfil);
            response.sendRedirect(
                    request.getContextPath() + "/paciente/inicio"
            );
        } catch (IllegalArgumentException | SecurityException error) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            request.setAttribute("error", error.getMessage());
            request.setAttribute("perfilGoogle", perfil);
            request.getRequestDispatcher(VISTA).forward(request, response);
        } catch (SQLException error) {
            log("No fue posible vincular la cuenta de Google.", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute(
                    "error",
                    "No fue posible vincular las cuentas en este momento."
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

    private static Long obtenerIdUsuario(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null) {
            return null;
        }
        Object valor = sesion.getAttribute(
                AutenticacionGoogleServlet.ATRIBUTO_USUARIO_PENDIENTE
        );
        return valor instanceof Long idUsuario ? idUsuario : null;
    }
}
