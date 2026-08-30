package py.fpune.soracare.api.dto;

import py.fpune.soracare.dto.SlotDisponibleDTO;

public record SlotDisponibleApiDTO(
        Long idHorario,
        Long idExcepcion,
        long idMedico,
        int idEspecialidad,
        long idConsultorio,
        String fecha,
        String horaInicio,
        String horaFin,
        boolean disponibilidadExtra
) {

    public static SlotDisponibleApiDTO desde(SlotDisponibleDTO slot) {
        return new SlotDisponibleApiDTO(
                slot.idHorario(),
                slot.idExcepcion(),
                slot.idMedico(),
                slot.idEspecialidad(),
                slot.idConsultorio(),
                slot.fecha().toString(),
                slot.horaInicio().toString(),
                slot.horaFin().toString(),
                slot.disponibilidadExtra()
        );
    }
}
