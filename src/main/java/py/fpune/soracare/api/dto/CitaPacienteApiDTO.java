package py.fpune.soracare.api.dto;

import py.fpune.soracare.mvc.model.Cita;

public record CitaPacienteApiDTO(
        long idCita,
        long idMedico,
        int idEspecialidad,
        long idConsultorio,
        String fechaHoraInicio,
        String fechaHoraFin,
        String codigoEstado,
        String nombreEstado,
        String motivoConsulta,
        String observaciones
) {

    public static CitaPacienteApiDTO desde(Cita cita) {
        return new CitaPacienteApiDTO(
                cita.getIdCita(),
                cita.getIdMedico(),
                cita.getIdEspecialidad(),
                cita.getIdConsultorio(),
                cita.getFechaHoraInicio().toString(),
                cita.getFechaHoraFin().toString(),
                cita.getCodigoEstado(),
                cita.getNombreEstado(),
                cita.getMotivoConsulta(),
                cita.getObservaciones()
        );
    }
}
