package py.fpune.soracare.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Cabeceras comunes sin alterar cookies de proveedores ni exigir HTTPS local. */
public class FiltroCabecerasSeguridad implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse http = (HttpServletResponse) response;
        HttpServletRequest solicitud = (HttpServletRequest) request;
        http.setHeader("X-Content-Type-Options", "nosniff");
        http.setHeader("X-Frame-Options", "SAMEORIGIN");
        http.setHeader("Content-Security-Policy", "frame-ancestors 'self'");
        // Login/registro conservan su excepción para Google Identity en localhost.
        http.setHeader("Referrer-Policy", "no-referrer");
        if (esHttps(solicitud)) {
            http.setHeader(
                    "Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains"
            );
        }
        chain.doFilter(request, response);
    }

    private static boolean esHttps(HttpServletRequest request) {
        String adelantado = request.getHeader("X-Forwarded-Proto");
        if (adelantado != null && !adelantado.isBlank()) {
            return "https".equalsIgnoreCase(
                    adelantado.split(",", 2)[0].strip()
            );
        }
        return request.isSecure();
    }
}
