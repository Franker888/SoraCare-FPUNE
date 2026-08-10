package py.fpune.soracare.dto;

public record RegistroExcepcionHorarioDTO(
        String medicoEspecialidad,
        String idConsultorio,
        String fecha,
        String horaInicio,
        String horaFin,
        String tipo,
        String motivo
) {
}
