package py.fpune.soracare.api.dto;

import py.fpune.soracare.dto.ResumenDisponibilidadMedicoDTO;

import java.util.List;

public record ResumenDisponibilidadMedicoApiDTO(
        long idMedico,
        String fechaSeleccionada,
        List<HorarioMedicoApiDTO> horarios,
        List<ExcepcionDisponibilidadMedicoApiDTO> excepciones,
        List<CitaAfectadaMedicoApiDTO> citasAfectadas
) {

    public static ResumenDisponibilidadMedicoApiDTO desde(
            ResumenDisponibilidadMedicoDTO resumen
    ) {
        return new ResumenDisponibilidadMedicoApiDTO(
                resumen.medico().getIdMedico(),
                resumen.fechaSeleccionada().toString(),
                resumen.horarios().stream()
                        .map(HorarioMedicoApiDTO::desde)
                        .toList(),
                resumen.excepciones().stream()
                        .map(ExcepcionDisponibilidadMedicoApiDTO::desde)
                        .toList(),
                resumen.citasAfectadas().stream()
                        .map(CitaAfectadaMedicoApiDTO::desde)
                        .toList()
        );
    }
}
