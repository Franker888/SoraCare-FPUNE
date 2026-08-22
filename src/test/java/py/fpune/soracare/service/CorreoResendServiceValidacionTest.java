package py.fpune.soracare.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class CorreoResendServiceValidacionTest {

    public static void main(String[] args) {
        debeEscaparContenidoYAdjuntarArchivos();
        debeProtegerElContenidoBinarioDelAdjunto();
        System.out.println("Validaciones de CorreoResendService: OK");
    }

    private static void debeEscaparContenidoYAdjuntarArchivos() {
        String json = CorreoResendService.construirJson(
                "SoraCare <onboarding@resend.dev>",
                "paciente@example.com",
                "Prueba \"SoraCare\"",
                "<p>Linea 1\nLinea 2</p>",
                List.of(new CorreoResendService.AdjuntoCorreo(
                        "indicacion.pdf",
                        "PDF".getBytes(StandardCharsets.UTF_8)
                ))
        );

        verificar(json.contains("Prueba \\\"SoraCare\\\""),
                "El asunto debe escapar comillas.");
        verificar(json.contains("Linea 1\\nLinea 2"),
                "El HTML debe escapar saltos de linea.");
        verificar(json.contains("\"filename\":\"indicacion.pdf\""),
                "El adjunto debe incluir su nombre.");
        verificar(json.contains("\"content\":\"UER G\"".replace(" ", "")),
                "El adjunto debe codificarse en Base64.");
    }

    private static void debeProtegerElContenidoBinarioDelAdjunto() {
        byte[] original = {1, 2, 3};
        CorreoResendService.AdjuntoCorreo adjunto =
                new CorreoResendService.AdjuntoCorreo("prueba.pdf", original);
        original[0] = 9;
        verificar(adjunto.contenido()[0] == 1,
                "El adjunto debe copiar el arreglo recibido.");
        byte[] lectura = adjunto.contenido();
        lectura[1] = 9;
        verificar(adjunto.contenido()[1] == 2,
                "El adjunto no debe exponer su arreglo interno.");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}
