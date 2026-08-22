package py.fpune.soracare.service;

public class ConfiguracionGoogleService {

    public static final String VARIABLE_CLIENT_ID =
            "SORACARE_GOOGLE_CLIENT_ID";

    public String obtenerClientId() {
        String valor = System.getenv(VARIABLE_CLIENT_ID);
        if (valor == null || valor.isBlank()) {
            return null;
        }

        String clientId = valor.strip();
        return clientId.endsWith(".apps.googleusercontent.com")
                ? clientId
                : null;
    }

    public boolean estaConfigurado() {
        return obtenerClientId() != null;
    }
}
