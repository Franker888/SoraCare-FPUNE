package py.fpune.soracare.api.dto;

import py.fpune.soracare.mvc.model.ExcepcionHorarioMedico;

public record ExcepcionDisponibilidadMedicoApiDTO(
        long idExcepcion,
        Integer idEspecialidad,
        Long idConsultorio,
        String fecha,
        String horaInicio,
        String horaFin,
        String tipo,
        String motivo,
        boolean activo
) {

    public static ExcepcionDisponibilidadMedicoApiDTO desde(
            ExcepcionHorarioMedico excepcion
    ) {
        return new ExcepcionDisponibilidadMedicoApiDTO(
                excepcion.getIdExcepcion(),
                excepcion.getIdEspecialidad(),
                excepcion.getIdConsultorio(),
                excepcion.getFecha().toString(),
                excepcion.getHoraInicio() == null
                        ? null
                        : excepcion.getHoraInicio().toString(),
                excepcion.getHoraFin() == null
                        ? null
                        : excepcion.getHoraFin().toString(),
                excepcion.getTipo().name(),
                excepcion.getMotivo(),
                excepcion.isActivo()
        );
    }
}
