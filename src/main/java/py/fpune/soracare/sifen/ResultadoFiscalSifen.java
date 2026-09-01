package py.fpune.soracare.sifen;

import java.math.BigDecimal;
import java.util.List;

public record ResultadoFiscalSifen(
        List<ItemCalculado> items,
        BigDecimal subtotalExento,
        BigDecimal subtotalExonerado,
        BigDecimal subtotalIva5,
        BigDecimal subtotalIva10,
        BigDecimal totalOperacion,
        BigDecimal iva5,
        BigDecimal iva10,
        BigDecimal totalIva,
        BigDecimal baseGravada5,
        BigDecimal baseGravada10,
        BigDecimal totalBaseGravada
) {
    public ResultadoFiscalSifen {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record ItemCalculado(
            ItemDocumentoSifen item,
            BigDecimal totalBruto,
            BigDecimal totalOperacion,
            BigDecimal baseGravada,
            BigDecimal ivaLiquidado,
            BigDecimal baseExenta
    ) {
    }
}
