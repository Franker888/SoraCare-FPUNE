package py.fpune.soracare.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record RecetaMedicaDTO(
        long idReceta,
        long idAtencion,
        long idPaciente,
        long idMedico,
        String codigo,
        OffsetDateTime fechaEmision,
        LocalDate fechaVencimiento,
        String indicacionesGenerales,
        String estado,
        OffsetDateTime fechaAnulacion,
        String motivoAnulacion,
        String nombrePaciente,
        String nombreMedico,
        String especialidad,
        List<DetalleRecetaDTO> detalles
) {
    public RecetaMedicaDTO {
        detalles = detalles == null ? List.of() : List.copyOf(detalles);
    }

    public boolean estaEmitida() {
        return "EMITIDA".equals(estado);
    }
}
