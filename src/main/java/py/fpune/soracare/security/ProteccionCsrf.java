package py.fpune.soracare.security;

import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class ProteccionCsrf {

    private static final String ATRIBUTO_TOKEN = "tokenCsrf";
    private static final int LONGITUD_TOKEN_BYTES = 32;
    private static final SecureRandom ALEATORIO_SEGURO = new SecureRandom();

    private ProteccionCsrf() {
    }

    public static String obtenerOCrear(HttpSession sesion) {
        if (sesion == null) {
            throw new IllegalArgumentException("No existe una sesión válida.");
        }
        Object guardado = sesion.getAttribute(ATRIBUTO_TOKEN);
        if (guardado instanceof String token && !token.isBlank()) {
            return token;
        }

        byte[] bytes = new byte[LONGITUD_TOKEN_BYTES];
        ALEATORIO_SEGURO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sesion.setAttribute(ATRIBUTO_TOKEN, token);
        return token;
    }

    public static boolean esValido(HttpSession sesion, String recibido) {
        if (sesion == null || recibido == null || recibido.isBlank()) {
            return false;
        }
        Object guardado = sesion.getAttribute(ATRIBUTO_TOKEN);
        if (!(guardado instanceof String esperado) || esperado.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8),
                recibido.getBytes(StandardCharsets.UTF_8)
        );
    }
}
