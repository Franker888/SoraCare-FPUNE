package py.fpune.soracare.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.junit.jupiter.api.Assertions.*;

class FiltroCabecerasSeguridadTest {
    @Test void registroPostFallidoConservaGoogleSinRedNiBase() throws Exception {
        var servlet = new py.fpune.soracare.mvc.controller.RegistroCuentaPacienteServlet();
        var turnstile = servlet.getClass().getDeclaredField("turnstileService");
        turnstile.setAccessible(true);
        turnstile.set(servlet, new py.fpune.soracare.service.TurnstileService() {
            @Override public void validar(String token, String accion) {
                throw new IllegalArgumentException("Verificación de prueba rechazada");
            }
            @Override public String obtenerSiteKey() { return null; }
        });
        var google = servlet.getClass().getDeclaredField("configuracionGoogleService");
        google.setAccessible(true);
        google.set(servlet, new py.fpune.soracare.service.ConfiguracionGoogleService() {
            @Override public String obtenerClientId() { return null; }
        });
        var request = new MockHttpServletRequest("POST", "/registro");
        var response = new MockHttpServletResponse();
        new FiltroCabecerasSeguridad().doFilter(request, response, servlet::service);
        assertEquals(400, response.getStatus());
        assertEquals("/WEB-INF/views/autenticacion/registro-paciente.jsp", response.getForwardedUrl());
        assertEquals("no-referrer-when-downgrade", response.getHeader("Referrer-Policy"));
    }

    @Test void protegeSinCrearSesionNiAlterarContenidoOCookiesDeGoogle() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        new FiltroCabecerasSeguridad().doFilter(request, response, (r, s) -> {
            s.setContentType("application/pdf");
            s.getWriter().write("contenido de prueba");
        });
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("SAMEORIGIN", response.getHeader("X-Frame-Options"));
        assertEquals("frame-ancestors 'self'", response.getHeader("Content-Security-Policy"));
        assertEquals("no-referrer", response.getHeader("Referrer-Policy"));
        assertEquals("application/pdf", response.getContentType());
        assertEquals("contenido de prueba", response.getContentAsString());
        assertNull(request.getSession(false));
        assertNull(response.getHeader("Set-Cookie"));
        assertNull(response.getHeader("Strict-Transport-Security"));
        assertNull(response.getHeader("Cross-Origin-Opener-Policy"));
    }

    @Test void conservaExcepcionDeReferrerDeGoogleLocal() throws Exception {
        var response = new MockHttpServletResponse();
        new FiltroCabecerasSeguridad().doFilter(new MockHttpServletRequest(), response,
                (r, s) -> ((jakarta.servlet.http.HttpServletResponse) s)
                        .setHeader("Referrer-Policy", "no-referrer-when-downgrade"));
        assertEquals("no-referrer-when-downgrade", response.getHeader("Referrer-Policy"));
    }
}
