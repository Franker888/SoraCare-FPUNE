package py.fpune.soracare.api.dto;

import py.fpune.soracare.dto.SolicitudReprogramacionCitaDTO;

public record ReprogramarCitaApiRequest(
        String idHorario,
        String idExcepcion,
        String idMedico,
        String idEspecialidad,
        String idConsultorio,
        String fecha,
        String horaInicio,
        String motivo
) {

    public SolicitudReprogramacionCitaDTO aSolicitudInterna(long idCita) {
        return new SolicitudReprogramacionCitaDTO(
                Long.toString(idCita),
                idHorario,
                idExcepcion,
                idMedico,
                idEspecialidad,
                idConsultorio,
                fecha,
                horaInicio,
                motivo
        );
    }
}
