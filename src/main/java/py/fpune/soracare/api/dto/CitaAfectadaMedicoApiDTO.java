package py.fpune.soracare.api.dto;

import py.fpune.soracare.dto.CitaAgendaMedicoDTO;

public record CitaAfectadaMedicoApiDTO(
        long idCita,
        String fechaHoraInicio,
        String fechaHoraFin,
        String pacienteNombreCompleto,
        String codigoEstado,
        String nombreEstado
) {

    public static CitaAfectadaMedicoApiDTO desde(
            CitaAgendaMedicoDTO cita
    ) {
        return new CitaAfectadaMedicoApiDTO(
                cita.idCita(),
                cita.fechaHoraInicio().toString(),
                cita.fechaHoraFin().toString(),
                cita.pacienteNombreCompleto(),
                cita.codigoEstado(),
                cita.nombreEstado()
        );
    }
}
