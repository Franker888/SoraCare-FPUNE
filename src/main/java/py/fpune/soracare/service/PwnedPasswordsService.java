package py.fpune.soracare.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Comprueba contrasenas nuevas contra Pwned Passwords mediante k-anonimato.
 *
 * <p>SoraCare calcula SHA-1 localmente y envia solamente los primeros cinco
 * caracteres del hash. La contrasena y el hash completo nunca abandonan el
 * servidor.</p>
 */
public class PwnedPasswordsService {

    private static final URI ENDPOINT_RANGO = URI.create(
            "https://api.pwnedpasswords.com/range/"
    );
    private static final String MENSAJE_COMPROMETIDA =
            "Esta contraseña apareció en filtraciones conocidas. "
                    + "Elegí otra contraseña única.";
    private static final String MENSAJE_NO_DISPONIBLE =
            "No pudimos verificar la seguridad de la contraseña. "
                    + "Intentá nuevamente en unos instantes.";

    private final HttpClient cliente;
    private final URI endpointRango;

    public PwnedPasswordsService() {
        this(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(8))
                        .build(),
                ENDPOINT_RANGO
        );
    }

    PwnedPasswordsService(
            HttpClient cliente,
            URI endpointRango
    ) {
        this.cliente = cliente;
        this.endpointRango = endpointRango;
    }

    /**
     * Rechaza una contrasena expuesta y falla de forma segura si la consulta
     * no puede completarse.
     */
    public void validar(String contrasena) {
        if (contrasena == null || contrasena.isEmpty()) {
            throw new IllegalArgumentException(
                    "La contraseña es obligatoria."
            );
        }

        String hash = calcularSha1(contrasena);
        String prefijo = hash.substring(0, 5);
        String sufijo = hash.substring(5);

        HttpRequest solicitud = HttpRequest.newBuilder(
                        endpointRango.resolve(prefijo)
                )
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "text/plain")
                .header("Add-Padding", "true")
                .header(
                        "User-Agent",
                        "SoraCare-FPUNE/1.0 password-security-check"
                )
                .GET()
                .build();

        HttpResponse<String> respuesta;
        try {
            respuesta = cliente.send(
                    solicitud,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
        } catch (IOException error) {
            throw new VerificacionContrasenaException(
                    MENSAJE_NO_DISPONIBLE,
                    error
            );
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new VerificacionContrasenaException(
                    MENSAJE_NO_DISPONIBLE,
                    error
            );
        }

        if (respuesta.statusCode() != 200) {
            throw new VerificacionContrasenaException(
                    MENSAJE_NO_DISPONIBLE
            );
        }

        boolean encontroLineaValida = false;
        for (String linea : respuesta.body().split("\\R")) {
            String lineaNormalizada = linea.strip();
            if (lineaNormalizada.isEmpty()) {
                continue;
            }

            int separador = lineaNormalizada.indexOf(':');
            if (separador != 35) {
                continue;
            }

            String sufijoRecibido = lineaNormalizada
                    .substring(0, separador)
                    .toUpperCase();
            long cantidad;
            try {
                cantidad = Long.parseLong(
                        lineaNormalizada.substring(separador + 1).strip()
                );
            } catch (NumberFormatException error) {
                continue;
            }

            encontroLineaValida = true;
            if (sufijo.equals(sufijoRecibido) && cantidad > 0) {
                throw new IllegalArgumentException(MENSAJE_COMPROMETIDA);
            }
        }

        if (!encontroLineaValida) {
            throw new VerificacionContrasenaException(
                    MENSAJE_NO_DISPONIBLE
            );
        }
    }

    private static String calcularSha1(String contrasena) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(
                    contrasena.getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of()
                    .withUpperCase()
                    .formatHex(hash);
        } catch (NoSuchAlgorithmException error) {
            throw new VerificacionContrasenaException(
                    MENSAJE_NO_DISPONIBLE,
                    error
            );
        }
    }
}
