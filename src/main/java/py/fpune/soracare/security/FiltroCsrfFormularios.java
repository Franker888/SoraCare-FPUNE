package py.fpune.soracare.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/** Intención de las solicitudes de navegador; no sustituye identidad ni permisos. */
public class FiltroCsrfFormularios implements Filter {
    private static final Set<String> SIN_VISTA = Set.of(
            "/conexion-bd", "/estado", "/firebase-messaging-sw.js");

    @Override
    public void doFilter(ServletRequest solicitud, ServletResponse respuesta, FilterChain cadena)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) solicitud;
        HttpServletResponse response = (HttpServletResponse) respuesta;
        // ServletPath ya está normalizado por el contenedor: no comparar la URI cruda.
        String ruta = request.getServletPath();
        if ("/api/v1".equals(ruta) || ruta.startsWith("/api/v1/")
                || "/auth/google".equals(ruta)) {
            // REST tiene X-CSRF-Token; Google verifica su propio g_csrf_token.
            cadena.doFilter(request, response);
            return;
        }
        boolean lectura = Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod());
        if (lectura && (ruta.startsWith("/assets/") || SIN_VISTA.contains(ruta))) {
            cadena.doFilter(request, response);
            return;
        }
        request.setCharacterEncoding("UTF-8");
        HttpSession sesion = request.getSession(false);
        if (!lectura) {
            boolean valido = false;
            if (sesion != null) {
                try {
                    if (request.getContentType() != null
                            && request.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("multipart/form-data")) {
                        // getParameter puede ocultar un error de tamaño del parser multipart.
                        request.getParts();
                    }
                    valido = ProteccionCsrf.esValido(sesion, tokenRecibido(request));
                } catch (IllegalStateException error) {
                    // El contenedor conserva los límites @MultipartConfig del servlet.
                    response.sendError(413, "El formulario supera el tamaño permitido.");
                    return;
                }
            }
            if (!valido) {
                response.setHeader("Cache-Control", "no-store");
                request.setAttribute("csrfInvalido", Boolean.TRUE);
                response.sendError(403, "Abrí nuevamente el formulario e intentá otra vez.");
                return;
            }
        }
        if (sesion == null) sesion = request.getSession(true);
        response.setHeader("Cache-Control", "no-store");
        request.setAttribute("csrfToken", ProteccionCsrf.obtenerOCrear(sesion));
        cadena.doFilter(request, response);
    }

    private static String tokenRecibido(HttpServletRequest request) {
        var cabeceras = Collections.list(request.getHeaders("X-CSRF-Token"));
        if (!cabeceras.isEmpty()) return cabeceras.size() == 1 ? cabeceras.getFirst() : null;
        String[] actual = request.getParameterValues("csrfToken");
        String[] anterior = request.getParameterValues("tokenCsrf");
        if (actual != null && anterior != null) return null;
        String[] tokens = actual == null ? anterior : actual;
        return tokens != null && tokens.length == 1 ? tokens[0] : null;
    }
}
