package py.fpune.soracare.dto;

import py.fpune.soracare.mvc.model.Paciente;

import java.util.List;

public record PanelClinicoPacienteDTO(
        Paciente paciente,
        CitaAgendaMedicoDTO cita,
        AtencionMedicaDTO atencionActual,
        List<AtencionMedicaDTO> historial
) {
    public PanelClinicoPacienteDTO {
        historial = List.copyOf(historial);
    }
}
