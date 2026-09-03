package py.fpune.soracare.service;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CifradoGoogleTokenServiceTest {

    @Test
    void cifraConNonceYRecuperaElToken() {
        byte[] clave = new byte[32];
        Arrays.fill(clave, (byte) 7);
        CifradoGoogleTokenService servicio =
                new CifradoGoogleTokenService(clave, new SecureRandom());

        String primero = servicio.cifrar("refresh-token-prueba");
        String segundo = servicio.cifrar("refresh-token-prueba");

        assertNotEquals(primero, segundo);
        assertEquals(
                "refresh-token-prueba",
                servicio.descifrar(primero)
        );
        assertEquals(
                "refresh-token-prueba",
                servicio.descifrar(segundo)
        );
    }

    @Test
    void rechazaUnTokenCifradoAlterado() {
        byte[] clave = Base64.getDecoder().decode(
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        );
        CifradoGoogleTokenService servicio =
                new CifradoGoogleTokenService(clave);
        String cifrado = servicio.cifrar("refresh-token-prueba");
        byte[] contenido = Base64.getUrlDecoder().decode(cifrado);
        contenido[contenido.length / 2] ^= 0x01;
        String alterado = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(contenido);

        assertThrows(
                SecurityException.class,
                () -> servicio.descifrar(alterado)
        );
    }
}
