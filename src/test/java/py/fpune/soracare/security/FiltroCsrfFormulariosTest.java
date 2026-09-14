package py.fpune.soracare.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import java.nio.file.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class FiltroCsrfFormulariosTest {
    private final FiltroCsrfFormularios filtro = new FiltroCsrfFormularios();

    @Test void protegeTodasLasMutacionesDeNavegadorSinDependerDelRol() throws Exception {
        for (String ruta : new String[]{"/mis-citas", "/agenda/pagos", "/medico/atencion",
                "/perfil", "/mis-documentos", "/medico/documentos/cargar", "/registro",
                "/google/registro", "/google/vincular", "/login", "/logout",
                "/cambiar-contrasena", "/cuenta/eliminar", "/api/sora/chat", "/ruta-nueva"}) {
            for (String metodo : new String[]{"POST", "PUT", "PATCH", "DELETE"}) {
                var r = solicitud(metodo, ruta);
                var s = new MockHttpServletResponse();
                var destino = new MockFilterChain();
                filtro.doFilter(r, s, destino);
                assertEquals(403, s.getStatus(), ruta);
                assertNull(destino.getRequest());
                assertNull(r.getSession(false));
            }
        }
    }

    @Test void permiteFormularioValidoYConservaAmbosContratosExistentes() throws Exception {
        for (String nombre : new String[]{"csrfToken", "tokenCsrf"}) {
            var r = solicitud("POST", "/mis-citas");
            String token = ProteccionCsrf.obtenerOCrear(r.getSession());
            r.setParameter(nombre, token);
            var destino = new MockFilterChain();
            filtro.doFilter(r, new MockHttpServletResponse(), destino);
            assertNotNull(destino.getRequest());
            assertEquals(token, r.getAttribute("csrfToken"));
            assertEquals("UTF-8", r.getCharacterEncoding());
        }
    }

    @Test void rechazaTokenIncorrectoAjenoYAmbiguo() throws Exception {
        for (int caso=0; caso<4; caso++) {
            var r=solicitud("POST", "/perfil");
            String token=ProteccionCsrf.obtenerOCrear(r.getSession());
            r.setParameter("csrfToken", switch(caso) {
                case 0 -> new String[]{"incorrecto"};
                case 1 -> new String[]{ProteccionCsrf.obtenerOCrear(new MockHttpSession())};
                case 2 -> new String[]{token, token};
                default -> new String[]{token};
            });
            if(caso==3) r.setParameter("tokenCsrf",token);
            var s=new MockHttpServletResponse();
            var destino=new MockFilterChain();
            filtro.doFilter(r,s,destino);
            assertEquals(403,s.getStatus());
            assertNull(destino.getRequest());
        }
    }

    @Test void jsonValidoNoConsumeElCuerpo() throws Exception {
        var r=solicitud("POST","/api/sora/chat");
        r.setContentType("application/json");
        r.setContent("{\"mensaje\":\"consulta ficticia\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        r.addHeader("X-CSRF-Token",ProteccionCsrf.obtenerOCrear(r.getSession()));
        var destino=new MockFilterChain();
        filtro.doFilter(r,new MockHttpServletResponse(),destino);
        assertNotNull(destino.getRequest());
        assertTrue(r.getReader().readLine().contains("consulta ficticia"));
    }

    @Test void conservaControlesDeGoogleYRestSinEximirSusVecinos() throws Exception {
        for(String ruta:new String[]{"/auth/google","/api/v1"}) {
            var r=solicitud("POST",ruta);
            var destino=new MockFilterChain();
            filtro.doFilter(r,new MockHttpServletResponse(),destino);
            assertNotNull(destino.getRequest());
            assertNull(r.getSession(false));
        }
        for(String ruta:new String[]{"/auth/google/otra","/api/v10","/google/calendar/conectar"}) {
            var s=new MockHttpServletResponse();
            filtro.doFilter(solicitud("POST",ruta),s,new MockFilterChain());
            assertEquals(403,s.getStatus());
        }
    }

    @Test void lecturaGeneraTokenPeroSaludYAssetsNoCreanSesiones() throws Exception {
        var r=solicitud("GET","/login");
        filtro.doFilter(r,new MockHttpServletResponse(),new MockFilterChain());
        assertTrue(ProteccionCsrf.esValido(r.getSession(),(String)r.getAttribute("csrfToken")));
        for(String ruta:new String[]{"/conexion-bd","/assets/app/js/sora-assistant.js"}) {
            r=solicitud("GET",ruta);
            filtro.doFilter(r,new MockHttpServletResponse(),new MockFilterChain());
            assertNull(r.getSession(false));
        }
    }

    @Test void todosLosFormulariosPostTienenTokenSinDuplicados() throws Exception {
        Pattern form=Pattern.compile("<form\\b[^>]*>[\\s\\S]*?</form>",Pattern.CASE_INSENSITIVE);
        Pattern token=Pattern.compile("name=[\"'](?:csrfToken|tokenCsrf|g_csrf_token)[\"']");
        int total=0;
        try(var archivos=Files.walk(Path.of("src/main/webapp"))) {
            for(Path archivo:archivos.filter(p->p.toString().matches(".*\\.jspf?")).toList()) {
                var m=form.matcher(Files.readString(archivo));
                while(m.find()) {
                    String f=m.group();
                    String apertura=f.substring(0,f.indexOf('>')+1);
                    if(!apertura.contains("method=\"post\""))continue;
                    assertEquals(1,token.matcher(f).results().count(),archivo.toString());
                    total++;
                }
            }
        }
        assertTrue(total>50,"No se inspeccionó todo el catálogo de formularios");
    }

    private static MockHttpServletRequest solicitud(String metodo,String ruta) {
        var r=new MockHttpServletRequest(metodo,"/SoraCare-FPUNE"+ruta);
        r.setContextPath("/SoraCare-FPUNE"); r.setServletPath(ruta); return r;
    }
}
