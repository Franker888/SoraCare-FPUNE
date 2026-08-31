package py.fpune.soracare.config;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConexionBDConfiguracionTest {

    @Test
    void mantieneLaConfiguracionLocalComoValorPredeterminado()
            throws Exception {
        ConexionBD.Configuracion configuracion =
                ConexionBD.resolverConfiguracion(
                        Map.of("SORACARE_DB_PASSWORD", "secreto")::get
                );

        assertTrue(configuracion.url().startsWith(
                "jdbc:postgresql://127.0.0.1:5432/soracare_db"
        ));
        assertEquals("soracare_app", configuracion.usuario());
    }

    @Test
    void permiteConfigurarElServidorDeDocker() throws Exception {
        Map<String, String> entorno = Map.of(
                "SORACARE_DB_URL",
                "jdbc:postgresql://postgres:5432/soracare_db",
                "SORACARE_DB_USER",
                "usuario_docker",
                "SORACARE_DB_PASSWORD",
                "secreto"
        );

        ConexionBD.Configuracion configuracion =
                ConexionBD.resolverConfiguracion(entorno::get);

        assertEquals(
                "jdbc:postgresql://postgres:5432/soracare_db",
                configuracion.url()
        );
        assertEquals("usuario_docker", configuracion.usuario());
    }

    @Test
    void rechazaUnaDireccionQueNoSeaPostgreSql() {
        Map<String, String> entorno = Map.of(
                "SORACARE_DB_URL", "jdbc:mysql://base/soracare",
                "SORACARE_DB_PASSWORD", "secreto"
        );

        assertThrows(
                SQLException.class,
                () -> ConexionBD.resolverConfiguracion(entorno::get)
        );
    }

    @Test
    void exigeLaContrasenaSinIncluirlaEnElCodigo() {
        SQLException error = assertThrows(
                SQLException.class,
                () -> ConexionBD.resolverConfiguracion(nombre -> null)
        );

        assertTrue(error.getMessage().contains("SORACARE_DB_PASSWORD"));
    }
}
