package py.fpune.soracare.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import py.fpune.soracare.dto.PerfilGoogleDTO;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

public class TokenGoogleService {

    private final ConfiguracionGoogleService configuracion;
    private volatile GoogleIdTokenVerifier verificador;
    private volatile String clientIdVerificador;

    public TokenGoogleService() {
        this.configuracion = new ConfiguracionGoogleService();
    }

    public PerfilGoogleDTO verificar(String credencial)
            throws IOException, GeneralSecurityException {
        if (credencial == null || credencial.isBlank()) {
            throw new SecurityException(
                    "Google no devolvió una credencial válida."
            );
        }

        String clientId = configuracion.obtenerClientId();
        if (clientId == null) {
            throw new IllegalStateException(
                    "El acceso con Google todavía no está configurado."
            );
        }

        GoogleIdToken token = obtenerVerificador(clientId)
                .verify(credencial.strip());
        if (token == null) {
            throw new SecurityException(
                    "No fue posible validar la identidad de Google."
            );
        }

        GoogleIdToken.Payload datos = token.getPayload();
        Boolean correoVerificado = datos.getEmailVerified();
        if (!Boolean.TRUE.equals(correoVerificado)) {
            throw new SecurityException(
                    "Google no confirmó el correo de esta cuenta."
            );
        }

        String identificador = normalizar(datos.getSubject(), 255);
        String correo = normalizar(datos.getEmail(), 150);
        if (identificador == null || correo == null) {
            throw new SecurityException(
                    "Google no proporcionó los datos mínimos de la cuenta."
            );
        }

        return new PerfilGoogleDTO(
                identificador,
                correo.toLowerCase(),
                true,
                normalizar((String) datos.get("given_name"), 100),
                normalizar((String) datos.get("family_name"), 100),
                normalizar((String) datos.get("name"), 200),
                normalizar((String) datos.get("picture"), 500),
                normalizar((String) datos.get("hd"), 255)
        );
    }

    private GoogleIdTokenVerifier obtenerVerificador(String clientId) {
        GoogleIdTokenVerifier actual = verificador;
        if (actual != null && clientId.equals(clientIdVerificador)) {
            return actual;
        }

        synchronized (this) {
            if (verificador == null
                    || !clientId.equals(clientIdVerificador)) {
                verificador = new GoogleIdTokenVerifier.Builder(
                        new NetHttpTransport(),
                        GsonFactory.getDefaultInstance()
                ).setAudience(Collections.singletonList(clientId)).build();
                clientIdVerificador = clientId;
            }
            return verificador;
        }
    }

    private static String normalizar(String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String texto = valor.strip();
        return texto.length() <= maximo
                ? texto
                : texto.substring(0, maximo);
    }
}
