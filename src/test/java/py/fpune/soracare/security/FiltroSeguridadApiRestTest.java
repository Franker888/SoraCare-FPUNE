package py.fpune.soracare.security;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import py.fpune.soracare.api.security.SeguridadApiRest;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FiltroSeguridadApiRestTest {

    private final FiltroSeguridadApiRest filtro =
            new FiltroSeguridadApiRest();

    @Test
    void permiteLosCatalogosPublicosDeSoloLectura() throws Exception {
        Resultado resultado = ejecutar(
                "GET",
                "/especialidades",
                null,
                false
        );

        assertNotNull(resultado.cadena().getRequest());
        assertEquals(
                "nosniff",
                resultado.response().getHeader("X-Content-Type-Options")
        );
    }

    @Test
    void rechazaLaSesionPrivadaSinAutenticacion() throws Exception {
        Resultado resultado = ejecutar("GET", "/sesion", null, false);

        assertEquals(401, resultado.response().getStatus());
        assertNull(resultado.cadena().getRequest());
        verificarCodigo(resultado, "NO_AUTENTICADO");
    }

    @Test
    void rechazaEscriturasDeCitasSinAutenticacion() throws Exception {
        Resultado resultado = ejecutar("POST", "/citas", null, false);

        assertEquals(401, resultado.response().getStatus());
        verificarCodigo(resultado, "NO_AUTENTICADO");
    }

    @Test
    void exigeTokenCsrfEnEscriturasAutenticadas() throws Exception {
        Resultado resultado = ejecutar(
                "POST",
                "/citas",
                usuario("PACIENTE"),
                false
        );

        assertEquals(403, resultado.response().getStatus());
        verificarCodigo(resultado, "CSRF_INVALIDO");
    }

    @Test
    void exigeRolPacienteParaOperacionesDeCitas() throws Exception {
        Resultado resultado = ejecutar(
                "POST",
                "/citas",
                usuario("MEDICO"),
                true
        );

        assertEquals(403, resultado.response().getStatus());
        verificarCodigo(resultado, "ACCESO_DENEGADO");
    }

    @Test
    void permiteContinuarConRolYTokenCorrectos() throws Exception {
        Resultado resultado = ejecutar(
                "POST",
                "/citas",
                usuario("PACIENTE"),
                true
        );

        assertNotNull(resultado.cadena().getRequest());
        assertEquals(200, resultado.response().getStatus());
    }

    @Test
    void protegeTambienLaCancelacionConCsrf() throws Exception {
        Resultado sinToken = ejecutar(
                "PATCH",
                "/citas/41/cancelacion",
                usuario("PACIENTE"),
                false
        );
        Resultado conToken = ejecutar(
                "PATCH",
                "/citas/41/cancelacion",
                usuario("PACIENTE"),
                true
        );

        assertEquals(403, sinToken.response().getStatus());
        verificarCodigo(sinToken, "CSRF_INVALIDO");
        assertNotNull(conToken.cadena().getRequest());
        assertEquals(200, conToken.response().getStatus());
    }

    @Test
    void protegeTambienLaReprogramacionConCsrf() throws Exception {
        Resultado sinToken = ejecutar(
                "PATCH",
                "/citas/41/reprogramacion",
                usuario("PACIENTE"),
                false
        );
        Resultado conToken = ejecutar(
                "PATCH",
                "/citas/41/reprogramacion",
                usuario("PACIENTE"),
                true
        );

        assertEquals(403, sinToken.response().getStatus());
        verificarCodigo(sinToken, "CSRF_INVALIDO");
        assertNotNull(conToken.cadena().getRequest());
        assertEquals(200, conToken.response().getStatus());
    }

    @Test
    void exigeRolMedicoParaConsultarSuDisponibilidad() throws Exception {
        Resultado paciente = ejecutar(
                "GET",
                "/medicos/me/disponibilidad",
                usuario("PACIENTE"),
                false
        );
        Resultado medico = ejecutar(
                "GET",
                "/medicos/me/disponibilidad",
                usuario("MEDICO"),
                false
        );

        assertEquals(403, paciente.response().getStatus());
        verificarCodigo(paciente, "ACCESO_DENEGADO");
        assertNotNull(medico.cadena().getRequest());
        assertEquals(200, medico.response().getStatus());
    }

    @Test
    void protegeLaGestionDeDisponibilidadMedicaConCsrf() throws Exception {
        Resultado sinToken = ejecutar(
                "POST",
                "/medicos/me/disponibilidad/bloqueos",
                usuario("MEDICO"),
                false
        );
        Resultado conToken = ejecutar(
                "POST",
                "/medicos/me/disponibilidad/bloqueos",
                usuario("MEDICO"),
                true
        );

        assertEquals(403, sinToken.response().getStatus());
        verificarCodigo(sinToken, "CSRF_INVALIDO");
        assertNotNull(conToken.cadena().getRequest());
        assertEquals(200, conToken.response().getStatus());
    }

    @Test
    void permiteConsultarCitasSolamenteConRolPaciente() throws Exception {
        Resultado resultado = ejecutar(
                "GET",
                "/citas",
                usuario("PACIENTE"),
                false
        );

        assertNotNull(resultado.cadena().getRequest());
        assertEquals(200, resultado.response().getStatus());
    }

    @Test
    void bloqueaEscriturasEnCatalogosPublicos() throws Exception {
        Resultado resultado = ejecutar(
                "POST",
                "/medicos",
                usuario("PACIENTE"),
                true
        );

        assertEquals(405, resultado.response().getStatus());
        verificarCodigo(resultado, "METODO_NO_PERMITIDO");
    }

    @Test
    void bloqueaPorDefectoRecursosNoDeclarados() throws Exception {
        Resultado resultado = ejecutar(
                "GET",
                "/recurso-futuro",
                usuario("ADMINISTRADOR"),
                false
        );

        assertEquals(404, resultado.response().getStatus());
        verificarCodigo(resultado, "RECURSO_NO_ENCONTRADO");
    }

    private Resultado ejecutar(
            String metodo,
            String ruta,
            UsuarioAutenticadoDTO usuario,
            boolean incluirTokenCsrf
    ) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                metodo,
                "/SoraCare-FPUNE/api/v1" + ruta
        );
        request.setContextPath("/SoraCare-FPUNE");
        request.setServletPath("/api/v1");
        request.setPathInfo(ruta);

        if (usuario != null) {
            MockHttpSession sesion = new MockHttpSession();
            sesion.setAttribute("usuarioAutenticado", usuario);
            request.setSession(sesion);

            if (incluirTokenCsrf) {
                request.addHeader(
                        SeguridadApiRest.CABECERA_CSRF,
                        ProteccionCsrf.obtenerOCrear(sesion)
                );
            }
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();
        filtro.doFilter(request, response, cadena);
        return new Resultado(response, cadena);
    }

    private static UsuarioAutenticadoDTO usuario(String rol) {
        return new UsuarioAutenticadoDTO(
                5L,
                7L,
                "usuario.prueba",
                false,
                Set.of(rol)
        );
    }

    private static void verificarCodigo(
            Resultado resultado,
            String codigo
    ) throws Exception {
        JsonObject respuesta = JsonParser.parseString(
                resultado.response().getContentAsString()
        ).getAsJsonObject();
        assertEquals(codigo, respuesta.get("codigo").getAsString());
    }

    private record Resultado(
            MockHttpServletResponse response,
            MockFilterChain cadena
    ) {
    }
}
