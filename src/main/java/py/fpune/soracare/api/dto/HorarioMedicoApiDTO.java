package py.fpune.soracare.api.dto;

import py.fpune.soracare.mvc.model.HorarioMedico;

public record HorarioMedicoApiDTO(
        long idHorario,
        int idEspecialidad,
        long idConsultorio,
        int diaSemana,
        String horaInicio,
        String horaFin,
        int duracionTurnoMinutos,
        String fechaDesde,
        String fechaHasta,
        boolean activo
) {

    public static HorarioMedicoApiDTO desde(HorarioMedico horario) {
        return new HorarioMedicoApiDTO(
                horario.getIdHorario(),
                horario.getIdEspecialidad(),
                horario.getIdConsultorio(),
                horario.getDiaSemana(),
                horario.getHoraInicio().toString(),
                horario.getHoraFin().toString(),
                horario.getDuracionTurnoMinutos(),
                horario.getFechaDesde().toString(),
                horario.getFechaHasta() == null
                        ? null
                        : horario.getFechaHasta().toString(),
                horario.isActivo()
        );
    }
}
