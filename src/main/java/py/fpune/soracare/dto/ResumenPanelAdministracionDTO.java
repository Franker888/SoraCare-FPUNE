package py.fpune.soracare.dto;

public record ResumenPanelAdministracionDTO(
        long usuarios,
        long usuariosBloqueados,
        long pacientes,
        long medicos,
        long medicamentos,
        long citasFuturas,
        long eventosHoy
) {
}
