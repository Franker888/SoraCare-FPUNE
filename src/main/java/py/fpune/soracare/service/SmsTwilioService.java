package py.fpune.soracare.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente mínimo para Twilio Programmable Messaging.
 *
 * <p>Las credenciales se obtienen exclusivamente desde variables de entorno.
 * Nunca se guardan en el repositorio ni se muestran en pantalla.</p>
 */
public class SmsTwilioService {

    private static final String VARIABLE_ACCOUNT_SID =
            "SORACARE_TWILIO_ACCOUNT_SID";
    private static final String VARIABLE_AUTH_TOKEN =
            "SORACARE_TWILIO_AUTH_TOKEN";
    private static final String VARIABLE_REMITENTE =
            "SORACARE_TWILIO_FROM";
    private static final Pattern ACCOUNT_SID = Pattern.compile(
            "^AC[0-9a-fA-F]{32}$"
    );
    private static final Pattern TELEFONO_E164 = Pattern.compile(
            "^\\+[1-9][0-9]{7,14}$"
    );
    private static final Pattern SID_RESPUESTA = Pattern.compile(
            "\\\"sid\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""
    );
    private static final Pattern ESTADO_RESPUESTA = Pattern.compile(
            "\\\"status\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""
    );

    private final HttpClient cliente;
    private final String accountSid;
    private final String authToken;
    private final String remitente;

    public SmsTwilioService() {
        this(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build(),
                System.getenv(VARIABLE_ACCOUNT_SID),
                System.getenv(VARIABLE_AUTH_TOKEN),
                System.getenv(VARIABLE_REMITENTE)
        );
    }

    SmsTwilioService(
            HttpClient cliente,
            String accountSid,
            String authToken,
            String remitente
    ) {
        this.cliente = cliente;
        this.accountSid = limpiar(accountSid);
        this.authToken = limpiar(authToken);
        this.remitente = limpiar(remitente);
    }

    public boolean estaConfigurado() {
        return ACCOUNT_SID.matcher(accountSid).matches()
                && !authToken.isBlank()
                && TELEFONO_E164.matcher(remitente).matches();
    }

    public String remitenteConfigurado() {
        return remitente.isBlank() ? "Sin número configurado" : remitente;
    }

    public ResultadoEnvio enviar(String destinatario, String mensaje)
            throws IOException {
        if (!estaConfigurado()) {
            throw new IllegalStateException(
                    "Twilio no está configurado. Definí las tres variables "
                            + "de entorno antes de iniciar Tomcat."
            );
        }

        String destinoValido = validarTelefono(destinatario);
        String mensajeValido = validarMensaje(mensaje);
        String formulario = construirFormulario(
                destinoValido, remitente, mensajeValido);
        String credenciales = Base64.getEncoder().encodeToString(
                (accountSid + ":" + authToken)
                        .getBytes(StandardCharsets.UTF_8)
        );
        URI endpoint = URI.create(
                "https://api.twilio.com/2010-04-01/Accounts/"
                        + accountSid + "/Messages.json"
        );

        HttpRequest solicitud = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Basic " + credenciales)
                .header(
                        "Content-Type",
                        "application/x-www-form-urlencoded;charset=UTF-8"
                )
                .POST(HttpRequest.BodyPublishers.ofString(
                        formulario, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> respuesta;
        try {
            respuesta = cliente.send(
                    solicitud,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IOException("El envío del SMS fue interrumpido.", error);
        }

        if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
            throw new IOException(
                    "Twilio rechazó el SMS con estado HTTP "
                            + respuesta.statusCode() + ". "
                            + resumirRespuesta(respuesta.body())
            );
        }

        String sid = extraer(SID_RESPUESTA, respuesta.body());
        String estado = extraer(ESTADO_RESPUESTA, respuesta.body());
        if (sid == null) {
            throw new IOException(
                    "Twilio aceptó la solicitud, pero no devolvió una referencia."
            );
        }
        return new ResultadoEnvio(
                sid,
                estado == null ? "aceptado" : estado,
                destinoValido,
                remitente
        );
    }

    static String construirFormulario(
            String destinatario,
            String remitente,
            String mensaje
    ) {
        return "To=" + codificar(destinatario)
                + "&From=" + codificar(remitente)
                + "&Body=" + codificar(mensaje);
    }

    static String validarTelefono(String valor) {
        String telefono = limpiar(valor).replace(" ", "");
        if (!TELEFONO_E164.matcher(telefono).matches()) {
            throw new IllegalArgumentException(
                    "El teléfono debe usar formato internacional, por ejemplo +595981234567."
            );
        }
        return telefono;
    }

    private static String validarMensaje(String valor) {
        String mensaje = limpiar(valor);
        if (mensaje.isBlank()) {
            throw new IllegalArgumentException("El mensaje es obligatorio.");
        }
        if (mensaje.length() > 1000) {
            throw new IllegalArgumentException(
                    "El mensaje supera el límite permitido para esta prueba."
            );
        }
        return mensaje;
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private static String extraer(Pattern patron, String respuesta) {
        if (respuesta == null) return null;
        Matcher coincidencia = patron.matcher(respuesta);
        return coincidencia.find() ? coincidencia.group(1) : null;
    }

    private static String resumirRespuesta(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            return "Sin detalle del proveedor.";
        }
        String resumen = cuerpo.replace('\r', ' ').replace('\n', ' ').strip();
        return resumen.length() <= 300
                ? resumen
                : resumen.substring(0, 300) + "...";
    }

    private static String limpiar(String valor) {
        return valor == null ? "" : valor.strip();
    }

    public record ResultadoEnvio(
            String referenciaProveedor,
            String estado,
            String destinatario,
            String remitente
    ) {
    }
}
