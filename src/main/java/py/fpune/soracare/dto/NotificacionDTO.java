package py.fpune.soracare.dto;

import java.time.OffsetDateTime;

public record NotificacionDTO(
        long idNotificacion,
        Long idCita,
        Long idPago,
        Long idReceta,
        String canal,
        String estado,
        String asunto,
        String contenido,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaLectura
) {
    public boolean estaLeida() {
        return fechaLectura != null;
    }
}
