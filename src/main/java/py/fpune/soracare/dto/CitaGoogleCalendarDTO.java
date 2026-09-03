package py.fpune.soracare.dto;

import java.time.OffsetDateTime;

public record CitaGoogleCalendarDTO(
        long idCita,
        String especialidad,
        String profesional,
        String ubicacion,
        OffsetDateTime inicio,
        OffsetDateTime fin
) {
}
