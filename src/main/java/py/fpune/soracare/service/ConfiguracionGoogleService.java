package py.fpune.soracare.service;

import java.net.URI;
import java.util.Base64;
import java.util.function.Function;

public class ConfiguracionGoogleService {

    public static final String VARIABLE_CLIENT_ID =
            "SORACARE_GOOGLE_CLIENT_ID";
    public static final String VARIABLE_CLIENT_SECRET =
            "SORACARE_GOOGLE_CLIENT_SECRET";
    public static final String VARIABLE_CALENDAR_REDIRECT_URI =
            "SORACARE_GOOGLE_CALENDAR_REDIRECT_URI";
    public static final String VARIABLE_TOKEN_ENCRYPTION_KEY =
            "SORACARE_GOOGLE_TOKEN_ENCRYPTION_KEY";

    private static final int LONGITUD_CLAVE_CIFRADO = 32;

    private final Function<String, String> entorno;

    public ConfiguracionGoogleService() {
        this(System::getenv);
    }

    ConfiguracionGoogleService(Function<String, String> entorno) {
        this.entorno = entorno;
    }

    public String obtenerClientId() {
        String valor = entorno.apply(VARIABLE_CLIENT_ID);
        if (valor == null || valor.isBlank()) {
            return null;
        }

        String clientId = valor.strip();
        return clientId.endsWith(".apps.googleusercontent.com")
                ? clientId
                : null;
    }

    public String obtenerClientSecret() {
        return textoOpcional(entorno.apply(VARIABLE_CLIENT_SECRET));
    }

    public URI obtenerCalendarRedirectUri() {
        String valor = textoOpcional(
                entorno.apply(VARIABLE_CALENDAR_REDIRECT_URI)
        );
        if (valor == null) {
            return null;
        }

        try {
            URI uri = URI.create(valor);
            String esquema = uri.getScheme();
            String host = uri.getHost();
            boolean esquemaValido = "https".equalsIgnoreCase(esquema)
                    || ("http".equalsIgnoreCase(esquema)
                    && esHostLocal(host));
            if (!uri.isAbsolute()
                    || !esquemaValido
                    || host == null
                    || uri.getUserInfo() != null
                    || uri.getFragment() != null) {
                return null;
            }
            return uri;
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    public byte[] obtenerClaveCifradoCalendar() {
        String valor = textoOpcional(
                entorno.apply(VARIABLE_TOKEN_ENCRYPTION_KEY)
        );
        if (valor == null) {
            return null;
        }

        try {
            byte[] clave = Base64.getDecoder().decode(valor);
            return clave.length == LONGITUD_CLAVE_CIFRADO
                    ? clave
                    : null;
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    public boolean estaConfigurado() {
        return obtenerClientId() != null;
    }

    public boolean calendarServidorConfigurado() {
        byte[] clave = obtenerClaveCifradoCalendar();
        try {
            return obtenerClientId() != null
                    && obtenerClientSecret() != null
                    && obtenerCalendarRedirectUri() != null
                    && clave != null;
        } finally {
            if (clave != null) {
                java.util.Arrays.fill(clave, (byte) 0);
            }
        }
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank()
                ? null
                : valor.strip();
    }

    private static boolean esHostLocal(String host) {
        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host);
    }
}
