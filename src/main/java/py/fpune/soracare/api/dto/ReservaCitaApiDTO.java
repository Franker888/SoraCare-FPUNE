package py.fpune.soracare.api.dto;

import py.fpune.soracare.dto.ResultadoReservaDTO;

public record ReservaCitaApiDTO(
        CitaPacienteApiDTO cita,
        PagoCitaApiDTO pago
) {

    public static ReservaCitaApiDTO desde(ResultadoReservaDTO resultado) {
        return new ReservaCitaApiDTO(
                CitaPacienteApiDTO.desde(resultado.cita()),
                PagoCitaApiDTO.desde(resultado.pago())
        );
    }
}
