package py.fpune.soracare.config;

import jakarta.servlet.ServletContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Function;

/** Configuración que diferencia el HTTP local de un despliegue HTTPS público. */
public final class ConfiguracionDespliegue {

    public static final String VARIABLE_ORIGEN_PUBLICO =
            "SORACARE_PUBLIC_ORIGIN";

    private final Function<String, String> entorno;

    public ConfiguracionDespliegue() {
        this(System::getenv);
    }

    ConfiguracionDespliegue(Function<String, String> entorno) {
        this.entorno = entorno;
    }

    public URI obtenerOrigenPublico() {
        String valor = entorno.apply(VARIABLE_ORIGEN_PUBLICO);
        if (valor == null || valor.isBlank()) return null;

        try {
            URI uri = URI.create(valor.strip());
            String esquema = uri.getScheme();
            String host = uri.getHost();
            boolean local = "localhost".equalsIgnoreCase(host)
                    || "127.0.0.1".equals(host)
                    || "::1".equals(host);
            boolean esquemaValido = "https".equalsIgnoreCase(esquema)
                    || (local && "http".equalsIgnoreCase(esquema));
            String ruta = uri.getPath();

            if (!uri.isAbsolute()
                    || !esquemaValido
                    || host == null
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null
                    || (ruta != null && !ruta.isBlank() && !"/".equals(ruta))) {
                throw configuracionInvalida();
            }

            return new URI(
                    esquema.toLowerCase(),
                    null,
                    host,
                    uri.getPort(),
                    null,
                    null,
                    null
            );
        } catch (IllegalArgumentException | URISyntaxException error) {
            throw new IllegalStateException(
                    mensajeConfiguracionInvalida(),
                    error
            );
        }
    }

    public void configurarCookieSesion(ServletContext contexto) {
        URI origen = obtenerOrigenPublico();
        contexto.getSessionCookieConfig().setSecure(
                origen != null && "https".equalsIgnoreCase(origen.getScheme())
        );
    }

    private static IllegalStateException configuracionInvalida() {
        return new IllegalStateException(mensajeConfiguracionInvalida());
    }

    private static String mensajeConfiguracionInvalida() {
        return VARIABLE_ORIGEN_PUBLICO
                + " debe ser un origen HTTPS sin ruta, consulta ni fragmento.";
    }
}
