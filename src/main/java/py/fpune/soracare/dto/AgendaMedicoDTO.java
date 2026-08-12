package py.fpune.soracare.dto;

import py.fpune.soracare.mvc.model.Medico;

import java.time.LocalDate;
import java.util.List;

public record AgendaMedicoDTO(
        Medico medico,
        LocalDate fecha,
        List<CitaAgendaMedicoDTO> citas
) {
    public AgendaMedicoDTO {
        citas = List.copyOf(citas);
    }
}
