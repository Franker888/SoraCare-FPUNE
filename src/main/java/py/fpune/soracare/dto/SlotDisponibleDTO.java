package py.fpune.soracare.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record SlotDisponibleDTO(
        Long idHorario,
        Long idExcepcion,
        long idMedico,
        int idEspecialidad,
        long idConsultorio,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        boolean disponibilidadExtra
) {
}
