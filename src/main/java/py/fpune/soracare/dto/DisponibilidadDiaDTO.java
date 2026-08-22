package py.fpune.soracare.dto;

import java.time.LocalDate;
import java.util.List;

public record DisponibilidadDiaDTO(
        LocalDate fecha,
        List<SlotDisponibleDTO> turnos
) {
    public DisponibilidadDiaDTO {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de disponibilidad es obligatoria.");
        }
        turnos = turnos == null ? List.of() : List.copyOf(turnos);
    }

    public int cantidadTurnos() {
        return turnos.size();
    }
}
