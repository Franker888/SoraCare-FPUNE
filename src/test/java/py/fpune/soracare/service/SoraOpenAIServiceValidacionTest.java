package py.fpune.soracare.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;

public final class SoraOpenAIServiceValidacionTest {

    private SoraOpenAIServiceValidacionTest() {
    }

    public static void main(String[] args) throws Exception {
        debeValidarYConstruirLaSolicitud();
        debeExtraerLaRespuestaDeLaApi();
        debeRechazarConsultasVacias();
        debeMantenerDisponibleElModoLocalSinClave();
        System.out.println("Validaciones de SoraOpenAIService: OK");
    }

    private static void debeValidarYConstruirLaSolicitud() {
        String entrada = SoraOpenAIService.construirEntrada(
                "¿Cómo reprogramo mi cita?",
                List.of(
                        new SoraOpenAIService.MensajeConversacion(
                                "usuario", "Tengo una cita reservada."
                        ),
                        new SoraOpenAIService.MensajeConversacion(
                                "asistente", "Podés verla en Mis citas."
                        )
                )
        );
        String cuerpo = SoraOpenAIService.construirCuerpoSolicitud(
                "modelo-prueba",
                entrada
        );
        JsonObject json = JsonParser.parseString(cuerpo).getAsJsonObject();

        verificar("modelo-prueba".equals(json.get("model").getAsString()),
                "La solicitud debe incluir el modelo configurado.");
        verificar(!json.get("store").getAsBoolean(),
                "La conversación no debe almacenarse mediante la API.");
        verificar(json.get("input").getAsString().contains("reprogramo"),
                "La solicitud debe incluir la consulta actual.");
    }

    private static void debeExtraerLaRespuestaDeLaApi() throws Exception {
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
        verificar(
                "Abrí Mis citas y elegí Reprogramar cita.".equals(
                        SoraOpenAIService.extraerTextoRespuesta(respuesta)
                ),
                "No se extrajo correctamente el texto de la respuesta."
        );
    }

    private static void debeRechazarConsultasVacias() {
        boolean rechazada = false;
        try {
            SoraOpenAIService.validarMensaje("   ");
        } catch (IllegalArgumentException esperado) {
            rechazada = true;
        }
        verificar(rechazada, "Sora no debe aceptar una consulta vacía.");
    }

    private static void debeMantenerDisponibleElModoLocalSinClave() {
        SoraOpenAIService servicio = new SoraOpenAIService(
                HttpClient.newHttpClient(),
                URI.create("https://example.test/responses"),
                "",
                null
        );
        verificar(!servicio.estaConfigurado(),
                "Sin clave, el cliente debe informar que la IA no está configurada.");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}
