package py.fpune.soracare;

import org.junit.jupiter.api.Test;
import py.fpune.soracare.security.CodificadorContrasenaBCryptTest;
import py.fpune.soracare.service.CitaServiceValidacionTest;
import py.fpune.soracare.service.ConfiguracionGoogleServiceValidacionTest;
import py.fpune.soracare.service.PagoServiceValidacionTest;
import py.fpune.soracare.service.PwnedPasswordsServiceValidacionTest;
import py.fpune.soracare.service.SentryMonitoreoServiceValidacionTest;
import py.fpune.soracare.service.TarjetaPruebaServiceValidacionTest;
import py.fpune.soracare.service.TurnstileServiceValidacionTest;

class ValidacionesCriticasTest {

    private static final String[] SIN_ARGUMENTOS = new String[0];

    @Test
    void validaCodificacionDeContrasenas() {
        CodificadorContrasenaBCryptTest.main(SIN_ARGUMENTOS);
    }

    @Test
    void validaReglasInicialesDeCitas() throws Exception {
        CitaServiceValidacionTest.main(SIN_ARGUMENTOS);
    }

    @Test
    void validaReglasInicialesDePagos() throws Exception {
        PagoServiceValidacionTest.main(SIN_ARGUMENTOS);
    }

    @Test
    void validaTarjetasDePrueba() {
        TarjetaPruebaServiceValidacionTest.main(SIN_ARGUMENTOS);
    }

    @Test
    void validaConfiguracionDeGoogle() {
        ConfiguracionGoogleServiceValidacionTest.main(SIN_ARGUMENTOS);
    }

    @Test
    void validaContrasenasFiltradas() throws Exception {
        PwnedPasswordsServiceValidacionTest.main(SIN_ARGUMENTOS);
    }

    @Test
    void validaAnonimizacionDeSentry() {
        SentryMonitoreoServiceValidacionTest.main(SIN_ARGUMENTOS);
    }

    @Test
    void validaTurnstileEnElServidor() throws Exception {
        TurnstileServiceValidacionTest.main(SIN_ARGUMENTOS);
    }
}
