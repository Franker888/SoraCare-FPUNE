package py.fpune.soracare.service;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public class TurnstileServiceValidacionTest {

    public static void main(String[] args) throws Exception {
        debePermitirElModoLocalSinClaves();
        debeRechazarUnaConfiguracionIncompleta();
        debeValidarElTokenYLaAccionEnElServidor();
        System.out.println("Validaciones de TurnstileService: OK");
    }

    private static void debePermitirElModoLocalSinClaves() throws IOException {
        TurnstileService servicio = new TurnstileService(
                HttpClient.newHttpClient(),
                URI.create("http://127.0.0.1/no-se-utiliza"),
                null,
                null
        );
        servicio.validar(null, "login");
        verificar(!servicio.estaConfigurado(),
                "El modo sin claves no debe presentarse como configurado.");
    }

    private static void debeRechazarUnaConfiguracionIncompleta() {
        TurnstileService servicio = new TurnstileService(
                HttpClient.newHttpClient(),
                URI.create("http://127.0.0.1/no-se-utiliza"),
                "site-key",
                null
        );
        try {
            servicio.validar("token", "login");
            throw new AssertionError(
                    "Una configuracion parcial no debe validar formularios."
            );
        } catch (IOException esperado) {
            verificar(servicio.tieneConfiguracionIncompleta(),
                    "Debe detectarse la configuracion incompleta.");
        }
    }

    private static void debeValidarElTokenYLaAccionEnElServidor()
            throws Exception {
        AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
        AtomicReference<String> respuesta = new AtomicReference<>(
                "{\"success\":true,\"action\":\"login\"}"
        );
        HttpServer servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );
        servidor.createContext("/siteverify", intercambio -> {
            cuerpoRecibido.set(new String(
                    intercambio.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            ));
            byte[] contenido = respuesta.get().getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().set(
                    "Content-Type",
                    "application/json"
            );
            intercambio.sendResponseHeaders(200, contenido.length);
            intercambio.getResponseBody().write(contenido);
            intercambio.close();
        });
        servidor.start();

        try {
            TurnstileService servicio = new TurnstileService(
                    HttpClient.newHttpClient(),
                    URI.create("http://127.0.0.1:"
                            + servidor.getAddress().getPort()
                            + "/siteverify"),
                    "site-key-prueba",
                    "secret-key-prueba"
            );

            servicio.validar("token-prueba", "login");
            verificar(cuerpoRecibido.get().contains("secret=secret-key-prueba"),
                    "La clave secreta debe enviarse al servidor de validacion.");
            verificar(cuerpoRecibido.get().contains("response=token-prueba"),
                    "El token del widget debe enviarse a Siteverify.");

            verificarRechazo(
                    servicio,
                    "token-prueba",
                    "registro",
                    "Un token de login no debe autorizar un registro."
            );

            respuesta.set(
                    "{\"success\":false,"
                            + "\"error-codes\":[\"invalid-input-response\"]}"
            );
            verificarRechazo(
                    servicio,
                    "token-invalido",
                    "login",
                    "Un token rechazado por Cloudflare no debe autorizar el acceso."
            );
        } finally {
            servidor.stop(0);
        }
    }

    private static void verificarRechazo(
            TurnstileService servicio,
            String token,
            String accion,
            String mensaje
    ) throws IOException {
        try {
            servicio.validar(token, accion);
            throw new AssertionError(mensaje);
        } catch (SecurityException esperado) {
            // Resultado esperado.
        }
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}
