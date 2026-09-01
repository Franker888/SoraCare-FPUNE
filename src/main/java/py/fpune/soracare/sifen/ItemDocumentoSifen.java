package py.fpune.soracare.sifen;

import java.math.BigDecimal;

public record ItemDocumentoSifen(
        String codigo,
        String descripcion,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        AfectacionIvaSifen afectacionIva,
        BigDecimal proporcionGravada,
        int tasaIva
) {
}
