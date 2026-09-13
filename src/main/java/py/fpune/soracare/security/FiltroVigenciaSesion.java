package py.fpune.soracare.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import py.fpune.soracare.dao.SesionVigenteDAO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import java.io.IOException;
import java.sql.SQLException;

/** Se registra primero en web.xml: protege toda copia de identidad antes de usarla. */
public class FiltroVigenciaSesion implements Filter {
    private final SesionVigenteDAO sesiones;

    public FiltroVigenciaSesion() { this(new SesionVigenteDAO()); }

    public FiltroVigenciaSesion(SesionVigenteDAO sesiones) { this.sesiones = sesiones; }

    @Override
    public void doFilter(ServletRequest solicitud, ServletResponse respuesta, FilterChain cadena)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) solicitud;
        HttpServletResponse response = (HttpServletResponse) respuesta;
        HttpSession sesion = request.getSession(false);
        Object identidad = sesion == null ? null : sesion.getAttribute("usuarioAutenticado");
        if (identidad != null) {
            boolean vigente;
            try {
                vigente = identidad instanceof UsuarioAutenticadoDTO usuario
                        && sesiones.esVigente(usuario.getIdUsuario(), usuario.getVersionSesion());
            } catch (SQLException error) {
                // Una caída de base no debe habilitar permisos conservados en memoria.
                request.getServletContext().log("No fue posible verificar la sesión.", error);
                responder(request, response, 503);
                return;
            }
            if (!vigente) {
                try { sesion.invalidate(); } catch (IllegalStateException ignorado) { }
                responder(request, response, 401);
                return;
            }
        }
        cadena.doFilter(request, response);
    }

    private static void responder(HttpServletRequest request, HttpServletResponse response,
                                  int estado) throws IOException {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        if (ruta.startsWith("/api/")) {
            response.setStatus(estado);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(estado == 401
                    ? "{\"estado\":401,\"codigo\":\"NO_AUTENTICADO\",\"mensaje\":\"Iniciá sesión nuevamente.\"}"
                    : "{\"estado\":503,\"codigo\":\"SERVICIO_NO_DISPONIBLE\",\"mensaje\":\"Intentá nuevamente más tarde.\"}");
        } else if (estado == 401) {
            response.sendRedirect(request.getContextPath() + "/login");
        } else {
            response.sendError(503, "No fue posible verificar la sesión. Intentá nuevamente.");
        }
    }
}
