package py.fpune.soracare.dto;

import java.time.OffsetDateTime;

public record VinculoGoogleCalendarDTO(
        long idUsuario,
        String correoGoogle,
        String refreshTokenCifrado,
        String alcance,
        OffsetDateTime actualizadoEn
) {
}
