package py.fpune.soracare.dto;

import java.time.OffsetDateTime;

public record CitaAgendaMedicoDTO(
        long idCita,
        long idPaciente,
        OffsetDateTime fechaHoraInicio,
        OffsetDateTime fechaHoraFin,
        String pacienteNombreCompleto,
        String cedulaPaciente,
        String numeroHistoriaClinica,
        String especialidad,
        String consultorio,
        String motivoConsulta,
        String observaciones,
        String codigoEstado,
        String nombreEstado
) {
}
