package py.fpune.soracare.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracionDespliegueTest {

    @Test
    void exigeHttpsParaUnOrigenPublico() {
        var insegura = new ConfiguracionDespliegue(
                Map.of(
                        ConfiguracionDespliegue.VARIABLE_ORIGEN_PUBLICO,
                        "http://soracare.example"
                )::get
        );

        assertThrows(
                IllegalStateException.class,
                insegura::obtenerOrigenPublico
        );
    }

    @Test
    void normalizaElOrigenHttpsSinAceptarRutas() {
        var configuracion = new ConfiguracionDespliegue(
                Map.of(
                        ConfiguracionDespliegue.VARIABLE_ORIGEN_PUBLICO,
                        "https://SoraCare.Example/"
                )::get
        );
        var conRuta = new ConfiguracionDespliegue(
                Map.of(
                        ConfiguracionDespliegue.VARIABLE_ORIGEN_PUBLICO,
                        "https://soracare.example/ruta"
                )::get
        );

        assertEquals(
                "https://SoraCare.Example",
                configuracion.obtenerOrigenPublico().toString()
        );
        assertThrows(
                IllegalStateException.class,
                conRuta::obtenerOrigenPublico
        );
    }

    @Test
    void activaSecureSoloCuandoExisteUnOrigenHttpsValido() {
        MockServletContext contextoPublico = new MockServletContext();
        new ConfiguracionDespliegue(
                Map.of(
                        ConfiguracionDespliegue.VARIABLE_ORIGEN_PUBLICO,
                        "https://soracare.example"
                )::get
        ).configurarCookieSesion(contextoPublico);

        MockServletContext contextoLocal = new MockServletContext();
        new ConfiguracionDespliegue(nombre -> null)
                .configurarCookieSesion(contextoLocal);

        assertTrue(contextoPublico.getSessionCookieConfig().isSecure());
        assertFalse(contextoLocal.getSessionCookieConfig().isSecure());
    }
}
