package py.fpune.soracare.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import py.fpune.soracare.dto.AutorizacionGoogleCalendarDTO;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class GoogleOAuthService {

    public static final String ALCANCE_CALENDAR =
            "https://www.googleapis.com/auth/calendar.events";

    private static final URI ENDPOINT_AUTORIZACION = URI.create(
            "https://accounts.google.com/o/oauth2/v2/auth"
    );
    private static final URI ENDPOINT_TOKEN = URI.create(
            "https://oauth2.googleapis.com/token"
    );
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final ConfiguracionGoogleService configuracion;
    private final HttpClient httpClient;
    private final URI endpointToken;

    public GoogleOAuthService() {
        this(
                new ConfiguracionGoogleService(),
                HttpClient.newBuilder()
                        .connectTimeout(TIMEOUT)
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build(),
                ENDPOINT_TOKEN
        );
    }

    GoogleOAuthService(
            ConfiguracionGoogleService configuracion,
            HttpClient httpClient,
            URI endpointToken
    ) {
        this.configuracion = configuracion;
        this.httpClient = httpClient;
        this.endpointToken = endpointToken;
    }

    public URI crearUrlAutorizacion(String estado, String loginHint) {
        exigirConfiguracion();
        if (estado == null || estado.isBlank()) {
            throw new IllegalArgumentException(
                    "El estado de seguridad OAuth es obligatorio."
            );
        }

        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("client_id", configuracion.obtenerClientId());
        parametros.put(
                "redirect_uri",
                configuracion.obtenerCalendarRedirectUri().toString()
        );
        parametros.put("response_type", "code");
        parametros.put(
                "scope",
                "openid email " + ALCANCE_CALENDAR
        );
        parametros.put("access_type", "offline");
        parametros.put("include_granted_scopes", "true");
        parametros.put("prompt", "consent");
        parametros.put("state", estado.strip());
        if (loginHint != null && !loginHint.isBlank()) {
            parametros.put("login_hint", loginHint.strip());
        }

        return URI.create(
                ENDPOINT_AUTORIZACION
                        + "?"
                        + codificarFormulario(parametros)
        );
    }

    public AutorizacionGoogleCalendarDTO intercambiarCodigo(String codigo)
            throws IOException, InterruptedException {
        if (codigo == null || codigo.isBlank()) {
            throw new SecurityException(
                    "Google no devolvió un código de autorización válido."
            );
        }
        exigirConfiguracion();

        Map<String, String> parametros = parametrosCliente();
        parametros.put("code", codigo.strip());
        parametros.put(
                "redirect_uri",
                configuracion.obtenerCalendarRedirectUri().toString()
        );
        parametros.put("grant_type", "authorization_code");
        return enviarToken(parametros, true);
    }

    public String renovarAccessToken(String refreshToken)
            throws IOException, InterruptedException {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new SecurityException(
                    "Google Calendar debe volver a vincularse."
            );
        }
        exigirConfiguracion();

        Map<String, String> parametros = parametrosCliente();
        parametros.put("refresh_token", refreshToken.strip());
        parametros.put("grant_type", "refresh_token");
        return enviarToken(parametros, false).accessToken();
    }

    private AutorizacionGoogleCalendarDTO enviarToken(
            Map<String, String> parametros,
            boolean requiereRefreshToken
    ) throws IOException, InterruptedException {
        HttpRequest solicitud = HttpRequest.newBuilder(endpointToken)
                .timeout(TIMEOUT)
                .header(
                        "Content-Type",
                        "application/x-www-form-urlencoded; charset=UTF-8"
                )
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        codificarFormulario(parametros),
                        StandardCharsets.UTF_8
                ))
                .build();

        HttpResponse<String> respuesta = httpClient.send(
                solicitud,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        return procesarRespuestaToken(
                respuesta.statusCode(),
                respuesta.body(),
                requiereRefreshToken
        );
    }

    static AutorizacionGoogleCalendarDTO procesarRespuestaToken(
            int estadoHttp,
            String cuerpo,
            boolean requiereRefreshToken
    ) {
        JsonObject json;
        try {
            json = JsonParser.parseString(
                    cuerpo == null || cuerpo.isBlank() ? "{}" : cuerpo
            ).getAsJsonObject();
        } catch (RuntimeException error) {
            throw new SecurityException(
                    "Google devolvió una respuesta de autorización inválida."
            );
        }

        if (estadoHttp < 200 || estadoHttp >= 300) {
            String codigo = texto(json, "error");
            if ("invalid_grant".equals(codigo)) {
                throw new SecurityException(
                        "La autorización de Google venció o fue revocada. Volvé a vincular Calendar."
                );
            }
            throw new SecurityException(
                    "Google no aceptó la autorización de Calendar."
            );
        }

        String accessToken = texto(json, "access_token");
        String refreshToken = texto(json, "refresh_token");
        String idToken = texto(json, "id_token");
        String alcance = texto(json, "scope");
        long expiracion = json.has("expires_in")
                ? json.get("expires_in").getAsLong()
                : 0;
        if (accessToken == null
                || (requiereRefreshToken && refreshToken == null)
                || (requiereRefreshToken && idToken == null)) {
            throw new SecurityException(
                    "Google no entregó una autorización reutilizable. Revocá el permiso anterior y vinculá Calendar nuevamente."
            );
        }

        return new AutorizacionGoogleCalendarDTO(
                accessToken,
                refreshToken,
                idToken,
                alcance == null ? ALCANCE_CALENDAR : alcance,
                expiracion
        );
    }

    private Map<String, String> parametrosCliente() {
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("client_id", configuracion.obtenerClientId());
        parametros.put(
                "client_secret",
                configuracion.obtenerClientSecret()
        );
        return parametros;
    }

    private void exigirConfiguracion() {
        if (!configuracion.calendarServidorConfigurado()) {
            throw new IllegalStateException(
                    "Google Calendar todavía no tiene completa su configuración segura de servidor."
            );
        }
    }

    private static String codificarFormulario(Map<String, String> valores) {
        StringBuilder salida = new StringBuilder();
        valores.forEach((clave, valor) -> {
            if (salida.length() > 0) {
                salida.append('&');
            }
            salida.append(codificar(clave))
                    .append('=')
                    .append(codificar(valor));
        });
        return salida.toString();
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private static String texto(JsonObject json, String propiedad) {
        if (!json.has(propiedad) || json.get(propiedad).isJsonNull()) {
            return null;
        }
        String valor = json.get(propiedad).getAsString();
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
