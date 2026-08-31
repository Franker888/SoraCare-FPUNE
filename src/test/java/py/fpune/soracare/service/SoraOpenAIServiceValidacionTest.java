package py.fpune.soracare.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoraOpenAIServiceValidacionTest {

    @Test
    void construyeUnaSolicitudPrivadaBreveYEstructurada() {
        String cuerpo = SoraOpenAIService.construirCuerpoSolicitud(
                "modelo-prueba",
                "¿Cómo reprogramo mi cita?",
                List.of(
                        new SoraOpenAIService.MensajeConversacion(
                                "usuario", "Tengo una cita reservada."
                        ),
                        new SoraOpenAIService.MensajeConversacion(
                                "asistente", "Podés verla en Mis citas."
                        )
                ),
                "identificador-anonimo",
                "Especialidades activas: Cardiología."
        );
        JsonObject json = JsonParser.parseString(cuerpo).getAsJsonObject();
        JsonArray entrada = json.getAsJsonArray("input");

        assertEquals("modelo-prueba", json.get("model").getAsString());
        assertFalse(json.get("store").getAsBoolean());
        assertEquals("low", json.getAsJsonObject("reasoning")
                .get("effort").getAsString());
        assertEquals("medium", json.getAsJsonObject("text")
                .get("verbosity").getAsString());
        assertEquals("identificador-anonimo",
                json.get("safety_identifier").getAsString());
        assertEquals(3, entrada.size());
        assertEquals("user", entrada.get(0).getAsJsonObject()
                .get("role").getAsString());
        assertEquals("assistant", entrada.get(1).getAsJsonObject()
                .get("role").getAsString());
        assertTrue(entrada.get(2).getAsJsonObject()
                .get("content").getAsString().contains("reprogramo"));
        assertTrue(json.get("instructions").getAsString()
                .contains("Cardiología"));
        assertTrue(json.get("instructions").getAsString()
                .contains("texto plano"));
    }

    @Test
    void extraeLaRespuestaAnidadaDeLaApi() throws Exception {
        String respuesta = """
                {
                  "output": [{
                    "type": "message",
                    "content": [{
                      "type": "output_text",
                      "text": "Abrí Mis citas y elegí Reprogramar cita."
                    }]
                  }]
                }
                """;

        assertEquals(
                "Abrí Mis citas y elegí Reprogramar cita.",
                SoraOpenAIService.extraerTextoRespuesta(respuesta)
        );
    }

    @Test
    void rechazaConsultasVaciasYRespuestasInvalidas() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SoraOpenAIService.validarMensaje("   ")
        );
        assertThrows(
                IOException.class,
                () -> SoraOpenAIService.extraerTextoRespuesta("no-es-json")
        );
    }

    @Test
    void mantieneDisponibleElModoLocalSinClave() {
        SoraOpenAIService servicio = new SoraOpenAIService(
                HttpClient.newHttpClient(),
                URI.create("https://example.test/responses"),
                "",
                null
        );

        assertFalse(servicio.estaConfigurado());
        assertEquals("gpt-5.6-terra", servicio.modeloConfigurado());
        assertThrows(
                IllegalStateException.class,
                () -> servicio.responder(
                        "Hola",
                        List.of(),
                        "sesion-prueba",
                        ""
                )
        );
    }
}
