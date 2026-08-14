package py.fpune.soracare.dto;

public record SolicitudReprogramacionCitaDTO(
        String idCita,
        String idHorario,
        String idExcepcion,
        String idMedico,
        String idEspecialidad,
        String idConsultorio,
        String fecha,
        String horaInicio,
        String motivo
) {
}
