package py.fpune.soracare.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JpaContextoTest {

    private static final String MENSAJE_OPERACION_OBLIGATORIA =
            "La operación JPA es obligatoria.";

    @Test
    void consultaRequiereUnaOperacion() {
        NullPointerException error = assertThrows(
                NullPointerException.class,
                () -> JpaContexto.ejecutarConsulta(null)
        );

        assertEquals(MENSAJE_OPERACION_OBLIGATORIA, error.getMessage());
    }

    @Test
    void transaccionRequiereUnaOperacion() {
        NullPointerException error = assertThrows(
                NullPointerException.class,
                () -> JpaContexto.ejecutarEnTransaccion(null)
        );

        assertEquals(MENSAJE_OPERACION_OBLIGATORIA, error.getMessage());
    }
}
