package py.fpune.soracare.dto;

import py.fpune.soracare.mvc.model.Cita;

public record ResultadoReservaDTO(
        Cita cita,
        PagoResumenDTO pago
) {
}
