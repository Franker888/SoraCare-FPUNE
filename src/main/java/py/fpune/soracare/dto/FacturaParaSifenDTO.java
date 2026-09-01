package py.fpune.soracare.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record FacturaParaSifenDTO(
        long idFactura,
        long idPago,
        String numeroInterno,
        String tipoDocumentoReceptor,
        String numeroDocumentoReceptor,
        String nombreReceptor,
        String correoReceptor,
        BigDecimal total,
        String moneda,
        OffsetDateTime fechaEmision,
        List<Detalle> detalles
) {
    public FacturaParaSifenDTO {
        detalles = detalles == null ? List.of() : List.copyOf(detalles);
    }

    public record Detalle(
            long idDetalle,
            String descripcion,
            BigDecimal cantidad,
            BigDecimal precioUnitario
    ) { }
}
