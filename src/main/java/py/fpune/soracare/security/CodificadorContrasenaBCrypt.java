package py.fpune.soracare.security;

import org.mindrot.jbcrypt.BCrypt;

public final class CodificadorContrasenaBCrypt {

    private static final int FACTOR_TRABAJO = 12;

    private CodificadorContrasenaBCrypt() {
        throw new IllegalStateException(
                "Esta clase no debe ser instanciada."
        );
    }

    public static String codificar(
            String contrasena
    ) {
        if (contrasena == null
                || contrasena.isBlank()) {

            throw new IllegalArgumentException(
                    "La contraseña no puede estar vacía."
            );
        }

        return BCrypt.hashpw(
                contrasena,
                BCrypt.gensalt(FACTOR_TRABAJO)
        );
    }

    public static boolean verificar(
            String contrasena,
            String hashGuardado
    ) {
        if (contrasena == null
                || contrasena.isBlank()
                || hashGuardado == null
                || hashGuardado.isBlank()) {

            return false;
        }

        try {
            return BCrypt.checkpw(
                    contrasena,
                    hashGuardado
            );

        } catch (IllegalArgumentException error) {
            return false;
        }
    }
}