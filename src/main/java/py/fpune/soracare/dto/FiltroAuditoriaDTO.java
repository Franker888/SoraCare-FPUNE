package py.fpune.soracare.dto;

import java.time.LocalDate;

public record FiltroAuditoriaDTO(
        LocalDate desde,
        LocalDate hasta,
        String tipoEvento,
        String modulo,
        String resultado,
        String busqueda
) {
}
