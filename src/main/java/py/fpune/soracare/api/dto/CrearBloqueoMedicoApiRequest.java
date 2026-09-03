package py.fpune.soracare.api.dto;

import py.fpune.soracare.dto.RegistroExcepcionHorarioDTO;

public record CrearBloqueoMedicoApiRequest(
        String fecha,
        String horaInicio,
        String horaFin,
        String tipo,
        String motivo
) {

    public RegistroExcepcionHorarioDTO aSolicitudInterna() {
        return new RegistroExcepcionHorarioDTO(
                "",
                "",
                fecha,
                horaInicio,
                horaFin,
                tipo,
                motivo
        );
    }
}
