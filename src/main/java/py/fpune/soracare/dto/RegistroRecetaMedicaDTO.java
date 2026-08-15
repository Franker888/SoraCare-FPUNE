package py.fpune.soracare.dto;

import java.util.List;

public record RegistroRecetaMedicaDTO(
        String fechaVencimiento,
        String indicacionesGenerales,
        List<RegistroDetalleRecetaDTO> detalles
) {
    public RegistroRecetaMedicaDTO {
        detalles = detalles == null ? List.of() : List.copyOf(detalles);
    }
}
