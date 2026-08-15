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

@WebFilter(
        filterName = "FiltroPortalPaciente",
        urlPatterns = {
                "/mis-citas", "/mis-citas/*", "/mis-documentos",
                "/mis-recetas", "/mis-recetas/*", "/mis-notificaciones", "/pagos/*"
        }
)
public class FiltroPortalPaciente implements Filter {

    @Override
    public void doFilter(ServletRequest solicitud, ServletResponse respuesta,
                         FilterChain cadena) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) solicitud;
        HttpServletResponse response = (HttpServletResponse) respuesta;
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        HttpSession sesion = request.getSession(false);
        Object usuario = sesion == null ? null : sesion.getAttribute("usuarioAutenticado");
        if (!(usuario instanceof UsuarioAutenticadoDTO autenticado)) {
            if (sesion != null) sesion.invalidate();
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (autenticado.isDebeCambiarContrasena()) {
            response.sendRedirect(request.getContextPath() + "/cambiar-contrasena");
            return;
        }
        if (!autenticado.tieneRol("PACIENTE")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Esta seccion corresponde al portal del paciente.");
            return;
        }
        cadena.doFilter(request, response);
    }
}
