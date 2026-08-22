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
import py.fpune.soracare.service.AuditoriaService;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@WebFilter(filterName = "FiltroAuditoriaSistema", urlPatterns = "/*")
public class FiltroAuditoriaSistema implements Filter {

    private static final Set<String> METODOS_AUDITADOS =
            Set.of("POST", "PUT", "PATCH", "DELETE");
    private final AuditoriaService auditoriaService = new AuditoriaService();

    @Override
    public void doFilter(
            ServletRequest solicitud,
            ServletResponse respuesta,
            FilterChain cadena
    ) throws IOException, ServletException {
        if (!(solicitud instanceof HttpServletRequest request)
                || !(respuesta instanceof HttpServletResponse response)
                || !METODOS_AUDITADOS.contains(request.getMethod())) {
            cadena.doFilter(solicitud, respuesta);
            return;
        }

        UsuarioAutenticadoDTO usuarioAntes = obtenerUsuario(request);
        cadena.doFilter(request, response);
        UsuarioAutenticadoDTO usuarioDespues = obtenerUsuario(request);
        UsuarioAutenticadoDTO usuario = usuarioDespues != null ? usuarioDespues : usuarioAntes;

        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        int estado = response.getStatus();
        String tipo = clasificarTipo(ruta, estado, usuarioDespues);
        String resultado = estado >= 400
                || ("/login".equals(ruta) && usuarioDespues == null)
                ? "FALLIDO"
                : "EXITOSO";
        String descripcion = describir(tipo, ruta, resultado);

        auditoriaService.registrarSilenciosamente(
                usuario == null ? null : usuario.getIdUsuario(),
                tipo,
                clasificarModulo(ruta),
                clasificarEntidad(ruta),
                descripcion,
                request.getRemoteAddr(),
                request.getMethod(),
                ruta,
                request.getHeader("User-Agent"),
                resultado,
                "FALLIDO".equals(resultado)
                        ? (estado >= 400
                            ? "Respuesta HTTP " + estado
                            : "Operación rechazada por la aplicación")
                        : null
        );
    }

    private static UsuarioAutenticadoDTO obtenerUsuario(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null) return null;
        Object valor = sesion.getAttribute("usuarioAutenticado");
        return valor instanceof UsuarioAutenticadoDTO usuario ? usuario : null;
    }

    private static String clasificarTipo(
            String ruta,
            int estado,
            UsuarioAutenticadoDTO usuarioDespues
    ) {
        if ("/login".equals(ruta)) {
            return estado >= 400 || usuarioDespues == null
                    ? "AUTENTICACION_FALLIDA"
                    : "INICIO_SESION";
        }
        if ("/logout".equals(ruta)) return "CIERRE_SESION";
        if (ruta.contains("registro") || ruta.endsWith("/cargar")) return "CREACION";
        return "MODIFICACION";
    }

    private static String clasificarModulo(String ruta) {
        if (ruta.startsWith("/admin")) return "ADMINISTRACION";
        if (ruta.startsWith("/recepcion")) return "RECEPCION";
        if (ruta.startsWith("/perfil")) return "PERFIL";
        if (ruta.startsWith("/medico")) return "ATENCION_MEDICA";
        if (ruta.startsWith("/pagos") || ruta.startsWith("/agenda/pagos")) return "PAGOS";
        if (ruta.startsWith("/agenda") || ruta.startsWith("/mis-citas")) return "CITAS";
        if (ruta.startsWith("/login") || ruta.startsWith("/logout")
                || ruta.startsWith("/cambiar-contrasena")) return "SEGURIDAD";
        return "PORTAL";
    }

    private static String clasificarEntidad(String ruta) {
        String limpia = ruta.replaceAll("^/+", "");
        if (limpia.isBlank()) return "sistema";
        String[] partes = limpia.split("/");
        String entidad = partes[partes.length - 1].replace('-', '_');
        return entidad.isBlank() ? "sistema" : entidad.toLowerCase(Locale.ROOT);
    }

    private static String describir(String tipo, String ruta, String resultado) {
        return switch (tipo) {
            case "INICIO_SESION" -> "Inicio de sesión exitoso";
            case "AUTENTICACION_FALLIDA" -> "Intento de inicio de sesión rechazado";
            case "CIERRE_SESION" -> "Cierre de sesión";
            default -> "Operación " + resultado.toLowerCase(Locale.ROOT) + " en " + ruta;
        };
    }
}
