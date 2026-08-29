package py.fpune.soracare.security;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProteccionCsrfTest {

    @Test
    void conservaUnTokenPorSesion() {
        HttpSession sesion = crearSesion();

        String primero = ProteccionCsrf.obtenerOCrear(sesion);
        String segundo = ProteccionCsrf.obtenerOCrear(sesion);

        assertEquals(primero, segundo);
        assertEquals(43, primero.length());
    }

    @Test
    void aceptaSolamenteElTokenDeLaMismaSesion() {
        HttpSession sesion = crearSesion();
        String token = ProteccionCsrf.obtenerOCrear(sesion);

        assertTrue(ProteccionCsrf.esValido(sesion, token));
        assertFalse(ProteccionCsrf.esValido(sesion, null));
        assertFalse(ProteccionCsrf.esValido(sesion, "token-distinto"));
    }

    @Test
    void generaTokensDiferentesEntreSesiones() {
        String primero = ProteccionCsrf.obtenerOCrear(crearSesion());
        String segundo = ProteccionCsrf.obtenerOCrear(crearSesion());

        assertNotEquals(primero, segundo);
    }

    private static HttpSession crearSesion() {
        Map<String, Object> atributos = new HashMap<>();
        return (HttpSession) Proxy.newProxyInstance(
                ProteccionCsrfTest.class.getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, metodo, argumentos) -> switch (metodo.getName()) {
                    case "getAttribute" -> atributos.get((String) argumentos[0]);
                    case "setAttribute" -> {
                        atributos.put((String) argumentos[0], argumentos[1]);
                        yield null;
                    }
                    case "removeAttribute" -> {
                        atributos.remove((String) argumentos[0]);
                        yield null;
                    }
                    case "toString" -> "SesionPrueba";
                    default -> valorPredeterminado(metodo.getReturnType());
                }
        );
    }

    private static Object valorPredeterminado(Class<?> tipo) {
        if (!tipo.isPrimitive()) return null;
        if (tipo == boolean.class) return false;
        if (tipo == byte.class) return (byte) 0;
        if (tipo == short.class) return (short) 0;
        if (tipo == int.class) return 0;
        if (tipo == long.class) return 0L;
        if (tipo == float.class) return 0F;
        if (tipo == double.class) return 0D;
        if (tipo == char.class) return '\0';
        return null;
    }
}
