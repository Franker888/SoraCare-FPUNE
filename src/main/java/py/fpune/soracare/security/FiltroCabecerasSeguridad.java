package py.fpune.soracare.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Cabeceras comunes sin alterar cookies de proveedores ni exigir HTTPS local. */
public class FiltroCabecerasSeguridad implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse http = (HttpServletResponse) response;
        http.setHeader("X-Content-Type-Options", "nosniff");
        http.setHeader("X-Frame-Options", "SAMEORIGIN");
        http.setHeader("Content-Security-Policy", "frame-ancestors 'self'");
        // Login/registro conservan su excepción para Google Identity en localhost.
        http.setHeader("Referrer-Policy", "no-referrer");
        chain.doFilter(request, response);
    }
}
