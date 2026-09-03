package py.fpune.soracare.service;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import py.fpune.soracare.dto.AutorizacionGoogleCalendarDTO;

import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleOAuthServiceTest {

    @Test
    void construyeUnaAutorizacionOfflineSinExponerElSecreto() {
        ConfiguracionGoogleService configuracion = configuracion();
        GoogleOAuthService servicio = new GoogleOAuthService(
                configuracion,
                null,
                URI.create("https://oauth2.googleapis.com/token")
        );

        String url = servicio.crearUrlAutorizacion(
                "estado-seguro",
                "paciente@example.com"
        ).toString();

        assertTrue(url.contains("access_type=offline"));
        assertTrue(url.contains("prompt=consent"));
        assertTrue(url.contains("estado-seguro"));
        assertTrue(url.contains("calendar.events"));
        assertFalse(url.contains("secreto-de-prueba"));
    }

    @Test
    void procesaLaRespuestaCompletaDelIntercambio() {
        JsonObject json = new JsonObject();
        json.addProperty("access_token", "access-prueba");
        json.addProperty("refresh_token", "refresh-prueba");
        json.addProperty("id_token", "id-prueba");
        json.addProperty("scope", GoogleOAuthService.ALCANCE_CALENDAR);
        json.addProperty("expires_in", 3600);

        AutorizacionGoogleCalendarDTO resultado =
                GoogleOAuthService.procesarRespuestaToken(
                        200,
                        json.toString(),
                        true
                );

        assertEquals("access-prueba", resultado.accessToken());
        assertEquals("refresh-prueba", resultado.refreshToken());
        assertEquals(3600, resultado.expiraEnSegundos());
    }

    @Test
    void exigeRefreshTokenEnLaVinculacionInicial() {
        assertThrows(
                SecurityException.class,
                () -> GoogleOAuthService.procesarRespuestaToken(
                        200,
                        "{\"access_token\":\"access\",\"id_token\":\"id\"}",
                        true
                )
        );
    }

    private static ConfiguracionGoogleService configuracion() {
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
        return new ConfiguracionGoogleService(entorno::get);
    }
}
