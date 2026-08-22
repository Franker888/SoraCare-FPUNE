package py.fpune.soracare.dto;

import java.time.OffsetDateTime;

public record HistorialConfiguracionDTO(
        long idHistorial,
        String nombreConfiguracion,
        String clave,
        String valorAnterior,
        String valorNuevo,
        String usuario,
        String motivo,
        OffsetDateTime fechaCambio
) {
}
