package py.fpune.soracare.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import py.fpune.soracare.dto.CitaGoogleCalendarDTO;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GoogleCalendarServiceTest {

    @Test
    void generaUnEventoMinimoSinEnviarDatosClinicos() {
        CitaGoogleCalendarDTO cita = new CitaGoogleCalendarDTO(
                42,
                "Cardiología",
                "Dra. Ana Pérez",
                "Consultorio 3 - Sede Central",
                OffsetDateTime.parse("2026-09-10T14:00:00-03:00"),
                OffsetDateTime.parse("2026-09-10T14:30:00-03:00")
        );

        String cuerpo = GoogleCalendarService.construirEventoJson(cita);
        JsonObject json = JsonParser.parseString(cuerpo).getAsJsonObject();

        assertEquals("Cita médica · Cardiología", json.get("summary").getAsString());
        assertEquals(
                "42",
                json.getAsJsonObject("extendedProperties")
                        .getAsJsonObject("private")
                        .get("soracareCitaId")
                        .getAsString()
        );
        assertFalse(cuerpo.contains("motivoConsulta"));
        assertFalse(cuerpo.contains("observaciones"));
    }
}
