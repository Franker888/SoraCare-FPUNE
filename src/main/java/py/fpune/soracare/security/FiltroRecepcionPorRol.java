package py.fpune.soracare.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;

import java.io.IOException;

@WebFilter(filterName = "FiltroRecepcionPorRol", urlPatterns = {"/recepcion/*"})
public class FiltroRecepcionPorRol implements Filter {

    @Override
    public void doFilter(
            ServletRequest solicitud,
            ServletResponse respuesta,
            FilterChain cadena
    ) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) solicitud;
        HttpServletResponse response = (HttpServletResponse) respuesta;

        response.setHeader(
                "Cache-Control",
                "no-store, no-cache, must-revalidate, max-age=0"
        );
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        HttpSession sesion = request.getSession(false);
        Object usuarioEnSesion = sesion == null
                ? null
                : sesion.getAttribute("usuarioAutenticado");

        if (!(usuarioEnSesion instanceof UsuarioAutenticadoDTO usuarioAutenticado)) {
            if (sesion != null) {
                sesion.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        if (usuarioAutenticado.isDebeCambiarContrasena()) {
            response.sendRedirect(request.getContextPath() + "/cambiar-contrasena");
            return;
        }

        if (!usuarioAutenticado.tieneRol("RECEPCIONISTA")) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "No tenés permisos para acceder al centro de recepción."
            );
            return;
        }

        cadena.doFilter(request, response);
    }
}
