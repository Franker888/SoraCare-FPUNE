package py.fpune.soracare.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente minimo para la API de Resend.
 *
 * <p>La clave nunca se guarda en el codigo: se obtiene de
 * {@code SORACARE_RESEND_API_KEY}. El remitente se configura mediante
 * {@code SORACARE_EMAIL_FROM}.</p>
 */
public class CorreoResendService {

    private static final String VARIABLE_API_KEY = "SORACARE_RESEND_API_KEY";
    private static final String VARIABLE_REMITENTE = "SORACARE_EMAIL_FROM";
    private static final String REMITENTE_PRUEBA =
            "SoraCare <onboarding@resend.dev>";
    private static final URI ENDPOINT_RESEND =
            URI.create("https://api.resend.com/emails");
    private static final Pattern CORREO = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ID_RESPUESTA = Pattern.compile(
            "\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""
    );
    private static final int MAXIMO_ADJUNTOS_BYTES = 28 * 1024 * 1024;

    private final HttpClient cliente;
    private final URI endpoint;
    private final String apiKey;
    private final String remitente;

    public CorreoResendService() {
        this(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build(),
                ENDPOINT_RESEND,
                System.getenv(VARIABLE_API_KEY),
                valorOAlternativa(
                        System.getenv(VARIABLE_REMITENTE),
                        REMITENTE_PRUEBA
                )
        );
    }

    CorreoResendService(
            HttpClient cliente,
            URI endpoint,
            String apiKey,
            String remitente
    ) {
        this.cliente = cliente;
        this.endpoint = endpoint;
        this.apiKey = apiKey == null ? "" : apiKey.strip();
        this.remitente = valorOAlternativa(remitente, REMITENTE_PRUEBA);
    }

    public boolean estaConfigurado() {
        return !apiKey.isBlank();
    }

    public boolean usaRemitenteDePrueba() {
        return REMITENTE_PRUEBA.equalsIgnoreCase(remitente);
    }

    public String remitenteConfigurado() {
        return remitente;
    }

    public ResultadoEnvio enviar(
            String destinatario,
            String asunto,
            String contenidoHtml,
            List<AdjuntoCorreo> adjuntos,
            String claveIdempotencia
    ) throws IOException {
        if (!estaConfigurado()) {
            throw new IllegalStateException(
                    "Resend no esta configurado. Define "
                            + VARIABLE_API_KEY + " antes de iniciar Tomcat."
            );
        }

        String destinatarioValido = validarCorreo(destinatario);
        String asuntoValido = validarTexto(asunto, 180, "El asunto es obligatorio.");
        String htmlValido = validarTexto(
                contenidoHtml,
                120_000,
                "El contenido del correo es obligatorio."
        );
        List<AdjuntoCorreo> adjuntosValidos = validarAdjuntos(adjuntos);
        String idempotencia = claveIdempotencia == null
                || claveIdempotencia.isBlank()
                ? UUID.randomUUID().toString()
                : validarTexto(
                        claveIdempotencia,
                        256,
                        "La clave de idempotencia no es valida."
                );

        String json = construirJson(
                remitente,
                destinatarioValido,
                asuntoValido,
                htmlValido,
                adjuntosValidos
        );

        HttpRequest solicitud = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencia)
                .POST(HttpRequest.BodyPublishers.ofString(
                        json,
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
            throw new IOException("El envio del correo fue interrumpido.", error);
        }

        if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
            throw new IOException(
                    "Resend rechazo el envio con estado HTTP "
                            + respuesta.statusCode() + ". "
                            + resumirRespuesta(respuesta.body())
            );
        }

        Matcher coincidencia = ID_RESPUESTA.matcher(respuesta.body());
        if (!coincidencia.find()) {
            throw new IOException(
                    "Resend acepto la solicitud, pero no devolvio una referencia."
            );
        }

        return new ResultadoEnvio(
                coincidencia.group(1),
                destinatarioValido,
                remitente
        );
    }

    static String construirJson(
            String remitente,
            String destinatario,
            String asunto,
            String contenidoHtml,
            List<AdjuntoCorreo> adjuntos
    ) {
        StringBuilder json = new StringBuilder(512);
        json.append('{')
                .append("\"from\":\"").append(escaparJson(remitente)).append("\",")
                .append("\"to\":[\"").append(escaparJson(destinatario)).append("\"],")
                .append("\"subject\":\"").append(escaparJson(asunto)).append("\",")
                .append("\"html\":\"").append(escaparJson(contenidoHtml)).append('\"');

        if (adjuntos != null && !adjuntos.isEmpty()) {
            json.append(",\"attachments\":[");
            for (int i = 0; i < adjuntos.size(); i++) {
                if (i > 0) json.append(',');
                AdjuntoCorreo adjunto = adjuntos.get(i);
                json.append('{')
                        .append("\"filename\":\"")
                        .append(escaparJson(adjunto.nombreArchivo()))
                        .append("\",\"content\":\"")
                        .append(Base64.getEncoder().encodeToString(adjunto.contenido()))
                        .append("\"}");
            }
            json.append(']');
        }

        return json.append('}').toString();
    }

    private static List<AdjuntoCorreo> validarAdjuntos(
            List<AdjuntoCorreo> adjuntos
    ) {
        if (adjuntos == null || adjuntos.isEmpty()) return List.of();

        List<AdjuntoCorreo> resultado = new ArrayList<>(adjuntos.size());
        long total = 0;
        for (AdjuntoCorreo adjunto : adjuntos) {
            if (adjunto == null) {
                throw new IllegalArgumentException("El adjunto no puede ser nulo.");
            }
            String nombre = validarTexto(
                    adjunto.nombreArchivo(),
                    180,
                    "El nombre del archivo adjunto es obligatorio."
            );
            byte[] contenido = adjunto.contenido();
            if (contenido == null || contenido.length == 0) {
                throw new IllegalArgumentException(
                        "El archivo adjunto no posee contenido."
                );
            }
            total += contenido.length;
            if (total > MAXIMO_ADJUNTOS_BYTES) {
                throw new IllegalArgumentException(
                        "Los archivos adjuntos superan el limite permitido."
                );
            }
            resultado.add(new AdjuntoCorreo(nombre, contenido.clone()));
        }
        return List.copyOf(resultado);
    }

    private static String validarCorreo(String valor) {
        String correo = validarTexto(
                valor,
                254,
                "El correo destinatario es obligatorio."
        );
        if (!CORREO.matcher(correo).matches()) {
            throw new IllegalArgumentException(
                    "El correo destinatario no posee un formato valido."
            );
        }
        return correo.toLowerCase();
    }

    private static String validarTexto(
            String valor,
            int maximo,
            String mensajeObligatorio
    ) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensajeObligatorio);
        }
        String normalizado = valor.strip();
        if (normalizado.length() > maximo) {
            throw new IllegalArgumentException(
                    "Uno de los datos del correo supera el largo permitido."
            );
        }
        return normalizado;
    }

    private static String escaparJson(String valor) {
        StringBuilder resultado = new StringBuilder(valor.length() + 16);
        for (int i = 0; i < valor.length(); i++) {
            char caracter = valor.charAt(i);
            switch (caracter) {
                case '\"' -> resultado.append("\\\"");
                case '\\' -> resultado.append("\\\\");
                case '\b' -> resultado.append("\\b");
                case '\f' -> resultado.append("\\f");
                case '\n' -> resultado.append("\\n");
                case '\r' -> resultado.append("\\r");
                case '\t' -> resultado.append("\\t");
                default -> {
                    if (caracter < 0x20) {
                        resultado.append(String.format("\\u%04x", (int) caracter));
                    } else {
                        resultado.append(caracter);
                    }
                }
            }
        }
        return resultado.toString();
    }

    private static String resumirRespuesta(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) return "Sin detalle del proveedor.";
        String resumen = cuerpo.replace('\r', ' ').replace('\n', ' ').strip();
        return resumen.length() <= 300 ? resumen : resumen.substring(0, 300) + "...";
    }

    private static String valorOAlternativa(String valor, String alternativa) {
        return valor == null || valor.isBlank() ? alternativa : valor.strip();
    }

    public record AdjuntoCorreo(String nombreArchivo, byte[] contenido) {
        public AdjuntoCorreo {
            contenido = contenido == null ? null : contenido.clone();
        }

        @Override
        public byte[] contenido() {
            return contenido == null ? null : contenido.clone();
        }
    }

    public record ResultadoEnvio(
            String referenciaProveedor,
            String destinatario,
            String remitente
    ) {
    }
}
