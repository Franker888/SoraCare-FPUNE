package py.fpune.soracare.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import py.fpune.soracare.service.SentryMonitoreoService;

import java.io.IOException;

public class SentryErroresFilter implements Filter {

    private static final String ATRIBUTO_CAPTURADO =
            SentryErroresFilter.class.getName() + ".capturado";

    @Override
    public void doFilter(
            ServletRequest solicitud,
            ServletResponse respuesta,
            FilterChain cadena
    ) throws IOException, ServletException {
        try {
            cadena.doFilter(solicitud, respuesta);
        } catch (Throwable error) {
            capturarUnaVez(solicitud, error);
            relanzar(error);
        }
    }

    private static void capturarUnaVez(
            ServletRequest solicitud,
            Throwable error
    ) {
        if (!(solicitud instanceof HttpServletRequest request)
                || request.getAttribute(ATRIBUTO_CAPTURADO) != null) {
            return;
        }
        request.setAttribute(ATRIBUTO_CAPTURADO, Boolean.TRUE);
        SentryMonitoreoService.capturar(
                error,
                request.getMethod(),
                request.getServletPath()
        );
    }

    private static void relanzar(Throwable error)
            throws IOException, ServletException {
        if (error instanceof IOException excepcion) {
            throw excepcion;
        }
        if (error instanceof ServletException excepcion) {
            throw excepcion;
        }
        if (error instanceof RuntimeException excepcion) {
            throw excepcion;
        }
        if (error instanceof Error excepcion) {
            throw excepcion;
        }
        throw new ServletException(
                "Ocurrió un error interno al procesar la solicitud.",
                error
        );
    }
}
