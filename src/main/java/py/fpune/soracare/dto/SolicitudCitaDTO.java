package py.fpune.soracare.dto;

public record SolicitudCitaDTO(
        String idPaciente,
        String idHorario,
        String idExcepcion,
        String idMedico,
        String idEspecialidad,
        String idConsultorio,
        String fecha,
        String horaInicio,
        String motivoConsulta,
        String observaciones,
        String canalReserva
) {
}
