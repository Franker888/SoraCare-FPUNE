package py.fpune.soracare.dto;

import py.fpune.soracare.mvc.model.ExcepcionHorarioMedico;
import py.fpune.soracare.mvc.model.HorarioMedico;
import py.fpune.soracare.mvc.model.Medico;

import java.time.LocalDate;
import java.util.List;

public record ResumenDisponibilidadMedicoDTO(
        Medico medico,
        LocalDate fechaSeleccionada,
        List<HorarioMedico> horarios,
        List<ExcepcionHorarioMedico> excepciones,
        List<CitaAgendaMedicoDTO> citasAfectadas
) {
    public ResumenDisponibilidadMedicoDTO {
        horarios = List.copyOf(horarios);
        excepciones = List.copyOf(excepciones);
        citasAfectadas = List.copyOf(citasAfectadas);
    }
}
