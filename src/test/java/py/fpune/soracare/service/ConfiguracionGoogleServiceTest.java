package py.fpune.soracare.service;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracionGoogleServiceTest {

    @Test
    void reconoceLaConfiguracionSeguraDeCalendar() {
        Map<String, String> entorno = entornoCompleto();
        ConfiguracionGoogleService configuracion =
                new ConfiguracionGoogleService(entorno::get);

        assertTrue(configuracion.estaConfigurado());
        assertTrue(configuracion.calendarServidorConfigurado());
    }

    @Test
    void rechazaHttpFueraDelEntornoLocal() {
        Map<String, String> entorno = entornoCompleto();
        entorno.put(
                ConfiguracionGoogleService.VARIABLE_CALENDAR_REDIRECT_URI,
                "http://soracare.example/google/calendar/callback"
        );

        assertFalse(
                new ConfiguracionGoogleService(entorno::get)
                        .calendarServidorConfigurado()
        );
    }

    private static Map<String, String> entornoCompleto() {
        Map<String, String> entorno = new HashMap<>();
        entorno.put(
                ConfiguracionGoogleService.VARIABLE_CLIENT_ID,
                "cliente.apps.googleusercontent.com"
        );
        entorno.put(
                ConfiguracionGoogleService.VARIABLE_CLIENT_SECRET,
                "secreto-de-prueba"
        );
        entorno.put(
                ConfiguracionGoogleService.VARIABLE_CALENDAR_REDIRECT_URI,
                "http://localhost:8081/SoraCare-FPUNE/google/calendar/callback"
        );
        entorno.put(
                ConfiguracionGoogleService.VARIABLE_TOKEN_ENCRYPTION_KEY,
                Base64.getEncoder().encodeToString(new byte[32])
        );
        return entorno;
    }
}
