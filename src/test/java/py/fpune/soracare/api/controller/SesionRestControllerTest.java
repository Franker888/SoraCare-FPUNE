package py.fpune.soracare.api.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import py.fpune.soracare.api.security.SeguridadApiRest;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.security.ProteccionCsrf;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class SesionRestControllerTest {

    @Test
    void entregaUnTokenCsrfSolamenteAlUsuarioAutenticado()
            throws Exception {
        MockHttpSession sesion = sesionConUsuario(
                usuario(false, "PACIENTE", "MEDICO")
        );

        MvcResult resultado = crearMockMvc().perform(
                        get("/sesion").session(sesion)
                )
                .andReturn();

        assertEquals(200, resultado.getResponse().getStatus());
        JsonObject respuesta = JsonParser.parseString(
                resultado.getResponse().getContentAsString()
        ).getAsJsonObject();
        assertTrue(respuesta.get("autenticado").getAsBoolean());
        assertEquals(
                "paciente.prueba",
                respuesta.get("nombreUsuario").getAsString()
        );
        JsonArray roles = respuesta.getAsJsonArray("roles");
        assertEquals(2, roles.size());
        assertEquals("MEDICO", roles.get(0).getAsString());
        assertEquals("PACIENTE", roles.get(1).getAsString());
        String token = respuesta.get("tokenCsrf").getAsString();
        assertTrue(ProteccionCsrf.esValido(sesion, token));
        assertEquals(
                SeguridadApiRest.CABECERA_CSRF,
                respuesta.get("cabeceraCsrf").getAsString()
        );
        assertFalse(respuesta.has("idUsuario"));
        assertFalse(respuesta.has("idPersona"));
        assertFalse(respuesta.has("contrasena"));
    }

    @Test
    void rechazaLaConsultaSinSesion() throws Exception {
        MvcResult resultado = crearMockMvc().perform(get("/sesion"))
                .andReturn();

        assertEquals(401, resultado.getResponse().getStatus());
        verificarCodigo(resultado, "NO_AUTENTICADO");
    }

    @Test
    void exigeCambiarLaContrasenaAntesDeUsarLaApiPrivada()
            throws Exception {
        MockHttpSession sesion = sesionConUsuario(
                usuario(true, "PACIENTE")
        );

        MvcResult resultado = crearMockMvc().perform(
                        get("/sesion").session(sesion)
                )
                .andReturn();

        assertEquals(403, resultado.getResponse().getStatus());
        verificarCodigo(resultado, "ACCESO_DENEGADO");
    }

    private static MockMvc crearMockMvc() {
        return MockMvcBuilders.standaloneSetup(new SesionRestController())
                .setControllerAdvice(new ApiRestExcepciones())
                .build();
    }

    private static MockHttpSession sesionConUsuario(
            UsuarioAutenticadoDTO usuario
    ) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute("usuarioAutenticado", usuario);
        return sesion;
    }

    private static UsuarioAutenticadoDTO usuario(
            boolean debeCambiarContrasena,
            String... roles
    ) {
        return new UsuarioAutenticadoDTO(
                5L,
                7L,
                "paciente.prueba",
                debeCambiarContrasena,
                Set.of(roles)
        );
    }

    private static void verificarCodigo(
            MvcResult resultado,
            String codigo
    ) throws Exception {
        JsonObject respuesta = JsonParser.parseString(
                resultado.getResponse().getContentAsString()
        ).getAsJsonObject();
        assertEquals(codigo, respuesta.get("codigo").getAsString());
    }
}
