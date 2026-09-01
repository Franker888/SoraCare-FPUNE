package py.fpune.soracare.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumenFacturacionMedicoDTO(
        List<PagoResumenDTO> pagos,
        long facturasEmitidas,
        long pendientesEmision,
        BigDecimal totalCobrado,
        BigDecimal totalFacturado
) {

    public ResumenFacturacionMedicoDTO {
        pagos = List.copyOf(pagos);
    }

    public static ResumenFacturacionMedicoDTO desde(
            List<PagoResumenDTO> pagos
    ) {
        List<PagoResumenDTO> copia = pagos == null
                ? List.of()
                : List.copyOf(pagos);
        long emitidas = copia.stream()
                .filter(pago -> pago.idFacturaInterna() != null)
                .count();
        BigDecimal cobrado = copia.stream()
                .map(PagoResumenDTO::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal facturado = copia.stream()
                .filter(pago -> pago.idFacturaInterna() != null)
                .map(PagoResumenDTO::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ResumenFacturacionMedicoDTO(
                copia,
                emitidas,
                copia.size() - emitidas,
                cobrado,
                facturado
        );
    }

    public static ResumenFacturacionMedicoDTO vacio() {
        return desde(List.of());
    }
}
