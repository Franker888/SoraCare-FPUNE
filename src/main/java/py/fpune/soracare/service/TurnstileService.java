package py.fpune.soracare.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Valida en el servidor los desafios emitidos por Cloudflare Turnstile.
 *
 * <p>La clave publica se expone solamente para renderizar el widget. La clave
 * secreta se obtiene de una variable de entorno y nunca se envia al navegador.</p>
 */
public class TurnstileService {

    public static final String VARIABLE_SITE_KEY =
            "SORACARE_TURNSTILE_SITE_KEY";
    public static final String VARIABLE_SECRET_KEY =
            "SORACARE_TURNSTILE_SECRET_KEY";

    private static final URI ENDPOINT_SITEVERIFY = URI.create(
            "https://challenges.cloudflare.com/turnstile/v0/siteverify"
    );
    private static final int MAXIMO_TOKEN = 2048;
    private static final Set<String> ERRORES_DE_CONFIGURACION = Set.of(
            "missing-input-secret",
            "invalid-input-secret",
            "bad-request",
            "internal-error"
    );

    private final HttpClient cliente;
    private final URI endpoint;
    private final String siteKey;
    private final String secretKey;

    public TurnstileService() {
        this(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(8))
                        .build(),
                ENDPOINT_SITEVERIFY,
                System.getenv(VARIABLE_SITE_KEY),
                System.getenv(VARIABLE_SECRET_KEY)
        );
    }

    TurnstileService(
            HttpClient cliente,
            URI endpoint,
            String siteKey,
            String secretKey
    ) {
        this.cliente = cliente;
        this.endpoint = endpoint;
        this.siteKey = normalizar(siteKey);
        this.secretKey = normalizar(secretKey);
    }

    public boolean estaConfigurado() {
        return !siteKey.isBlank() && !secretKey.isBlank();
    }

    public boolean tieneConfiguracionIncompleta() {
        return siteKey.isBlank() != secretKey.isBlank();
    }

    public String obtenerSiteKey() {
        return estaConfigurado() ? siteKey : null;
    }

    /**
     * Valida el token si Turnstile esta configurado. Sin claves, SoraCare sigue
     * funcionando en modo local; con una configuracion parcial, falla de forma
     * segura para evitar una proteccion aparente.
     */
    public void validar(String token, String accionEsperada)
            throws IOException {
        if (tieneConfiguracionIncompleta()) {
            throw new IOException(
                    "La configuracion de Turnstile esta incompleta."
            );
        }
        if (!estaConfigurado()) {
            return;
        }

        String tokenNormalizado = normalizar(token);
        if (tokenNormalizado.isBlank()
                || tokenNormalizado.length() > MAXIMO_TOKEN) {
            throw new SecurityException(
                    "Completá la verificación de seguridad e intentá nuevamente."
            );
        }

        String cuerpo = "secret=" + codificar(secretKey)
                + "&response=" + codificar(tokenNormalizado)
                + "&idempotency_key=" + codificar(UUID.randomUUID().toString());

        HttpRequest solicitud = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(12))
                .header(
                        "Content-Type",
                        "application/x-www-form-urlencoded"
                )
                .POST(HttpRequest.BodyPublishers.ofString(
                        cuerpo,
                        StandardCharsets.UTF_8
                ))
                .build();

        HttpResponse<String> respuesta;
        try {
            respuesta = cliente.send(
                    solicitud,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "La validacion de Turnstile fue interrumpida.",
                    error
            );
        }

        if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
            throw new IOException(
                    "Turnstile respondio con estado HTTP "
                            + respuesta.statusCode() + "."
            );
        }

        JsonObject json;
        try {
            json = JsonParser.parseString(respuesta.body()).getAsJsonObject();
        } catch (RuntimeException error) {
            throw new IOException(
                    "Turnstile devolvio una respuesta no valida.",
                    error
            );
        }

        boolean exito = json.has("success")
                && json.get("success").getAsBoolean();
        if (!exito) {
            if (contieneErrorTecnico(json)) {
                throw new IOException(
                        "Turnstile no pudo validar el desafio por un error tecnico."
                );
            }
            throw new SecurityException(
                    "La verificación de seguridad venció o no fue válida. "
                            + "Volvé a intentarlo."
            );
        }

        String accion = json.has("action")
                ? normalizar(json.get("action").getAsString())
                : "";
        if (!normalizar(accionEsperada).equals(accion)) {
            throw new SecurityException(
                    "La verificación de seguridad no corresponde a este formulario."
            );
        }
    }

    private static boolean contieneErrorTecnico(JsonObject json) {
        if (!json.has("error-codes")
                || !json.get("error-codes").isJsonArray()) {
            return false;
        }
        return json.getAsJsonArray("error-codes")
                .asList()
                .stream()
                .map(elemento -> elemento.getAsString())
                .anyMatch(ERRORES_DE_CONFIGURACION::contains);
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : valor.strip();
    }
}
