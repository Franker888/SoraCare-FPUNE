package py.fpune.soracare.service;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class CifradoGoogleTokenService {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final byte VERSION = 1;
    private static final int LONGITUD_NONCE = 12;
    private static final int LONGITUD_TAG_BITS = 128;
    private static final byte[] DATOS_ASOCIADOS =
            "SoraCare-Google-Calendar-v1".getBytes(StandardCharsets.UTF_8);

    private final byte[] clave;
    private final SecureRandom aleatorio;

    public CifradoGoogleTokenService(byte[] clave) {
        this(clave, new SecureRandom());
    }

    CifradoGoogleTokenService(byte[] clave, SecureRandom aleatorio) {
        if (clave == null || clave.length != 32) {
            throw new IllegalArgumentException(
                    "La clave de cifrado de Google Calendar debe tener 32 bytes."
            );
        }
        this.clave = Arrays.copyOf(clave, clave.length);
        this.aleatorio = aleatorio;
    }

    public String cifrar(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "El token de Google Calendar es obligatorio."
            );
        }

        byte[] nonce = new byte[LONGITUD_NONCE];
        byte[] texto = token.getBytes(StandardCharsets.UTF_8);
        aleatorio.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(clave, "AES"),
                    new GCMParameterSpec(LONGITUD_TAG_BITS, nonce)
            );
            cipher.updateAAD(DATOS_ASOCIADOS);
            byte[] cifrado = cipher.doFinal(texto);
            ByteBuffer salida = ByteBuffer.allocate(
                    1 + nonce.length + cifrado.length
            );
            salida.put(VERSION).put(nonce).put(cifrado);
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(salida.array());
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException(
                    "No fue posible proteger la autorización de Google Calendar.",
                    error
            );
        } finally {
            Arrays.fill(texto, (byte) 0);
        }
    }

    public String descifrar(String tokenCifrado) {
        if (tokenCifrado == null || tokenCifrado.isBlank()) {
            throw new IllegalArgumentException(
                    "La autorización guardada de Google Calendar no es válida."
            );
        }

        byte[] contenido;
        try {
            contenido = Base64.getUrlDecoder().decode(tokenCifrado);
        } catch (IllegalArgumentException error) {
            throw tokenInvalido(error);
        }
        if (contenido.length <= 1 + LONGITUD_NONCE + 16
                || contenido[0] != VERSION) {
            throw tokenInvalido(null);
        }

        byte[] nonce = Arrays.copyOfRange(
                contenido,
                1,
                1 + LONGITUD_NONCE
        );
        byte[] cifrado = Arrays.copyOfRange(
                contenido,
                1 + LONGITUD_NONCE,
                contenido.length
        );
        try {
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(clave, "AES"),
                    new GCMParameterSpec(LONGITUD_TAG_BITS, nonce)
            );
            cipher.updateAAD(DATOS_ASOCIADOS);
            byte[] texto = cipher.doFinal(cifrado);
            try {
                return new String(texto, StandardCharsets.UTF_8);
            } finally {
                Arrays.fill(texto, (byte) 0);
            }
        } catch (AEADBadTagException error) {
            throw tokenInvalido(error);
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException(
                    "No fue posible leer la autorización de Google Calendar.",
                    error
            );
        } finally {
            Arrays.fill(contenido, (byte) 0);
            Arrays.fill(nonce, (byte) 0);
            Arrays.fill(cifrado, (byte) 0);
        }
    }

    private static SecurityException tokenInvalido(Throwable causa) {
        return new SecurityException(
                "La autorización guardada de Google Calendar fue alterada o ya no es válida.",
                causa
        );
    }
}
