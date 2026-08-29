package py.fpune.soracare.service;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;

public class PwnedPasswordsServiceValidacionTest {

    public static void main(String[] args) throws Exception {
        AtomicReference<String> respuesta = new AtomicReference<>();
        AtomicReference<String> rutaRecibida = new AtomicReference<>();
        AtomicReference<String> paddingRecibido = new AtomicReference<>();
        AtomicReference<String> agenteRecibido = new AtomicReference<>();

        HttpServer servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );
        servidor.createContext("/range/", intercambio -> {
            rutaRecibida.set(intercambio.getRequestURI().getPath());
            paddingRecibido.set(
                    intercambio.getRequestHeaders().getFirst("Add-Padding")
            );
            agenteRecibido.set(
                    intercambio.getRequestHeaders().getFirst("User-Agent")
            );
            byte[] contenido = respuesta.get()
                    .getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(200, contenido.length);
            intercambio.getResponseBody().write(contenido);
            intercambio.close();
        });
        servidor.start();

        try {
            PwnedPasswordsService servicio = new PwnedPasswordsService(
                    HttpClient.newHttpClient(),
                    URI.create(
                            "http://127.0.0.1:"
                                    + servidor.getAddress().getPort()
                                    + "/range/"
                    )
            );

            String contrasenaComprometida = "Password123!";
            String hash = sha1(contrasenaComprometida);
            respuesta.set(hash.substring(5) + ":42\n"
                    + "00000000000000000000000000000000000:0\n");

            verificarRechazo(servicio, contrasenaComprometida);
            verificar(
                    rutaRecibida.get().endsWith(hash.substring(0, 5)),
                    "Debe enviarse unicamente el prefijo de cinco caracteres."
            );
            verificar(
                    !rutaRecibida.get().contains(hash),
                    "El hash completo no debe enviarse al servicio externo."
            );
            verificar(
                    "true".equals(paddingRecibido.get()),
                    "La consulta debe solicitar respuestas con padding."
            );
            verificar(
                    agenteRecibido.get() != null
                            && agenteRecibido.get().contains("SoraCare-FPUNE"),
                    "La integracion debe identificarse mediante User-Agent."
            );

            String contrasenaSegura = "Clave-Unica-SoraCare-2026!";
            respuesta.set(
                    "00000000000000000000000000000000000:0\n"
                            + "11111111111111111111111111111111111:3\n"
            );
            servicio.validar(contrasenaSegura);

            respuesta.set("respuesta-invalida");
            verificarErrorTecnico(servicio, contrasenaSegura);
        } finally {
            servidor.stop(0);
        }

        System.out.println("Validaciones de PwnedPasswordsService: OK");
    }

    private static void verificarRechazo(
            PwnedPasswordsService servicio,
            String contrasena
    ) {
        try {
            servicio.validar(contrasena);
            throw new AssertionError(
                    "Una contrasena comprometida debe ser rechazada."
            );
        } catch (IllegalArgumentException esperado) {
            verificar(
                    esperado.getMessage().contains("filtraciones conocidas"),
                    "El rechazo debe explicar el riesgo sin exponer datos."
            );
        }
    }

    private static void verificarErrorTecnico(
            PwnedPasswordsService servicio,
            String contrasena
    ) {
        try {
            servicio.validar(contrasena);
            throw new AssertionError(
                    "Una respuesta invalida no debe habilitar la contrasena."
            );
        } catch (VerificacionContrasenaException esperado) {
            // Resultado esperado: falla cerrada.
        }
    }

    private static String sha1(String valor) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-1")
                .digest(valor.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().withUpperCase().formatHex(hash);
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}
