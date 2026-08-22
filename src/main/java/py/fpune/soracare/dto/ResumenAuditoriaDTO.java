package py.fpune.soracare.dto;

import java.util.List;

public record ResumenAuditoriaDTO(
        List<EventoAuditoriaDTO> eventos,
        long total,
        long exitosos,
        long fallidos,
        long usuariosDistintos,
        List<String> modulos
) {
}
