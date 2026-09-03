package py.fpune.soracare.api.dto;

import py.fpune.soracare.dto.PagoResumenDTO;

import java.math.BigDecimal;

public record PagoCitaApiDTO(
        long idPago,
        String codigoMetodo,
        String nombreMetodo,
        String codigoEstado,
        String nombreEstado,
        BigDecimal monto,
        String moneda,
        boolean esDigital,
        boolean esEfectivo
) {

    public static PagoCitaApiDTO desde(PagoResumenDTO pago) {
        return new PagoCitaApiDTO(
                pago.idPago(),
                pago.codigoMetodo(),
                pago.nombreMetodo(),
                pago.codigoEstado(),
                pago.nombreEstado(),
                pago.monto(),
                pago.moneda(),
                pago.esDigital(),
                pago.esEfectivo()
        );
    }
}
