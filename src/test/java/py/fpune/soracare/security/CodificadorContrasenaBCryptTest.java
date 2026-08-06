package py.fpune.soracare.security;

public class CodificadorContrasenaBCryptTest {

    public static void main(String[] args) {

        String contrasenaCorrecta =
                "SoraCare-Prueba-2026!";

        String contrasenaIncorrecta =
                "Clave-Incorrecta";

        String hash =
                CodificadorContrasenaBCrypt.codificar(
                        contrasenaCorrecta
                );

        boolean correctaAceptada =
                CodificadorContrasenaBCrypt.verificar(
                        contrasenaCorrecta,
                        hash
                );

        boolean incorrectaAceptada =
                CodificadorContrasenaBCrypt.verificar(
                        contrasenaIncorrecta,
                        hash
                );

        if (hash.equals(contrasenaCorrecta)) {
            throw new AssertionError(
                    "El hash no puede ser igual a la contraseña original."
            );
        }

        if (!correctaAceptada) {
            throw new AssertionError(
                    "BCrypt rechazó la contraseña correcta."
            );
        }

        if (incorrectaAceptada) {
            throw new AssertionError(
                    "BCrypt aceptó una contraseña incorrecta."
            );
        }

        System.out.println(
                "Prueba BCrypt completada correctamente."
        );

        System.out.println(
                "La contraseña correcta fue aceptada."
        );

        System.out.println(
                "La contraseña incorrecta fue rechazada."
        );

        System.out.println(
                "La contraseña original no se almacena directamente."
        );
    }
}