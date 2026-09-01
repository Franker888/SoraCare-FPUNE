import org.mindrot.jbcrypt.BCrypt;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Utilidad interna para el cargador local de datos demostrativos.
 * La contrasena se recibe por la entrada estandar y nunca se imprime.
 */
public final class GenerarHashBCrypt {

    private GenerarHashBCrypt() {
    }

    public static void main(String[] args) throws Exception {
        try (BufferedReader entrada = new BufferedReader(
                new InputStreamReader(System.in))) {
            String contrasena = entrada.readLine();
            if (contrasena == null || contrasena.isBlank()) {
                throw new IllegalArgumentException(
                        "La contrasena no puede estar vacia.");
            }
            System.out.println(BCrypt.hashpw(
                    contrasena,
                    BCrypt.gensalt(12)));
        }
    }
}
