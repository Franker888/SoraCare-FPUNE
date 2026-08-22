package py.fpune.soracare.dto;

import java.time.OffsetDateTime;

public record ConfiguracionSistemaDTO(
        long idConfiguracion,
        String clave,
        String nombre,
        String valor,
        String tipoDato,
        String categoria,
        String descripcion,
        boolean publica,
        boolean editable,
        boolean activa,
        OffsetDateTime actualizadaEn
) {
}
