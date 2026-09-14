package py.fpune.soracare.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;

import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class CsrfAdministrativoTest {
    private final FiltroAutorizacionPorRol filtro = new FiltroAutorizacionPorRol();

    @Test
    void postSinTokenNoLlegaAlCambioDeCuenta() throws Exception {
        var request = administrador("POST", "/admin/usuarios");
        request.setParameter("accion", "estado");
        request.setParameter("idUsuario", "8");
        request.setParameter("nuevoEstado", "ACTIVO");
        var response = new MockHttpServletResponse();
        var destino = new MockFilterChain();
        filtro.doFilter(request, response, destino);
        assertEquals(403, response.getStatus());
        assertNull(destino.getRequest());
        assertEquals(Boolean.TRUE, request.getAttribute("csrfInvalido"));
    }

    @Test
    void postConTokenDeLaSesionMantieneOperacionLegitima() throws Exception {
        var request = administrador("POST", "/admin/usuarios");
        request.setParameter("csrfToken", ProteccionCsrf.obtenerOCrear(request.getSession()));
        var destino = new MockFilterChain();
        filtro.doFilter(request, new MockHttpServletResponse(), destino);
        assertNotNull(destino.getRequest());
        assertEquals("UTF-8", request.getCharacterEncoding());
        assertEquals(request.getParameter("csrfToken"), request.getAttribute("csrfToken"));
    }

    @Test
    void rechazaTokensInvalidosAjenosYDuplicadosEnTodaMutacion() throws Exception {
        String ajeno = ProteccionCsrf.obtenerOCrear(new MockHttpSession());
        for (String metodo : new String[]{"POST", "PUT", "PATCH", "DELETE"}) {
            for (String ruta : new String[]{"/admin/usuarios", "/admin/configuracion",
                    "/admin/medicos", "/admin/horarios", "/admin/excepciones-horarios",
                    "/admin/integraciones/correo", "/admin/integraciones/sms",
                    "/admin/integraciones/sentry", "/admin/medicamentos",
                    "/admin/sedes", "/admin/consultorios", "/admin/especialidades"}) {
                for (int caso = 0; caso < 4; caso++) {
                    var request = administrador(metodo, ruta);
                    String valido = ProteccionCsrf.obtenerOCrear(request.getSession());
                    request.setParameter("csrfToken", switch (caso) {
                        case 0 -> new String[]{""};
                        case 1 -> new String[]{"incorrecto"};
                        case 2 -> new String[]{ajeno};
                        default -> new String[]{valido, "duplicado"};
                    });
                    var response = new MockHttpServletResponse();
                    var destino = new MockFilterChain();
                    filtro.doFilter(request, response, destino);
                    assertEquals(403, response.getStatus(), metodo + " " + ruta);
                    assertNull(destino.getRequest());
                    assertFalse(((MockHttpSession) request.getSession()).isInvalid());
                }
            }
        }
    }

    @Test
    void lecturaEntregaTokenEstableSinExigirloEnLaUrl() throws Exception {
        var request = administrador("GET", "/admin/usuarios");
        var response = new MockHttpServletResponse();
        var destino = new MockFilterChain();
        filtro.doFilter(request, response, destino);
        assertNotNull(destino.getRequest());
        assertTrue(ProteccionCsrf.esValido(request.getSession(),
                (String) request.getAttribute("csrfToken")));
        String token = (String) request.getAttribute("csrfToken");
        filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertEquals(token, request.getAttribute("csrfToken"));
        assertTrue(response.getHeader("Cache-Control").contains("no-store"));
    }

    @Test
    void tokenNoSustituyeAutenticacionRolNiCambioObligatorio() throws Exception {
        var anonimo = new MockHttpServletRequest("POST", "/admin/usuarios");
        var response = new MockHttpServletResponse();
        var destino = new MockFilterChain();
        filtro.doFilter(anonimo, response, destino);
        assertEquals("/login", response.getRedirectedUrl());
        assertNull(anonimo.getSession(false));
        assertNull(destino.getRequest());
        for (boolean cambio : new boolean[]{false, true}) {
            var request = administrador("POST", "/admin/usuarios");
            request.getSession().setAttribute("usuarioAutenticado",
                    new UsuarioAutenticadoDTO(7, 8, "test", cambio,
                            Set.of(cambio ? "ADMINISTRADOR" : "PACIENTE")));
            request.setParameter("csrfToken", ProteccionCsrf.obtenerOCrear(request.getSession()));
            response = new MockHttpServletResponse();
            destino = new MockFilterChain();
            filtro.doFilter(request, response, destino);
            assertNull(destino.getRequest());
            assertNull(request.getAttribute("csrfInvalido"));
            if (cambio) assertEquals("/SoraCare-FPUNE/cambiar-contrasena", response.getRedirectedUrl());
            else assertEquals(403, response.getStatus());
        }
    }

    @Test
    void todasLasVistasAdministrativasPostIncluyenToken() throws Exception {
        // Guard de mantenimiento: evita olvidar el campo al añadir un formulario.
        Pattern formularios = Pattern.compile("<form\\b[^>]*>[\\s\\S]*?</form>",
                Pattern.CASE_INSENSITIVE);
        int revisados = 0;
        try (var archivos = Files.list(Path.of("src/main/webapp/WEB-INF/views/admin"))) {
            for (Path archivo : archivos.filter(p -> p.toString().endsWith(".jsp")).toList()) {
                var matcher = formularios.matcher(Files.readString(archivo));
                while (matcher.find()) {
                    String formulario = matcher.group();
                    String apertura = formulario.substring(0, formulario.indexOf('>') + 1);
                    if (!apertura.contains("method=\"post\"") || apertura.contains("/logout")) continue;
                    assertTrue(formulario.contains("name=\"csrfToken\" value=\"${requestScope.csrfToken}\""),
                            archivo.toString());
                    assertEquals(1, formulario.split("name=\"csrfToken\"", -1).length - 1);
                    revisados++;
                }
            }
        }
        assertEquals(18, revisados);
    }

    private static MockHttpServletRequest administrador(String metodo, String ruta) {
        var request = new MockHttpServletRequest(metodo, "/SoraCare-FPUNE" + ruta);
        request.setContextPath("/SoraCare-FPUNE");
        request.setServletPath(ruta);
        request.setSession(new MockHttpSession());
        request.getSession().setAttribute("usuarioAutenticado",
                new UsuarioAutenticadoDTO(7, 8, "test", false, Set.of("ADMINISTRADOR")));
        return request;
    }
}
