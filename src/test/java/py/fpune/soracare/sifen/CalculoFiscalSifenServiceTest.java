package py.fpune.soracare.sifen;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculoFiscalSifenServiceTest {

    private final CalculoFiscalSifenService servicio =
            new CalculoFiscalSifenService();

    @Test
    void calculaIvaIncluidoAlDiezPorCiento() {
        ResultadoFiscalSifen resultado = servicio.calcular(List.of(
                new ItemDocumentoSifen(
                        "CONS-001",
                        "Consulta médica",
                        BigDecimal.ONE,
                        new BigDecimal("1100000"),
                        AfectacionIvaSifen.GRAVADO,
                        new BigDecimal("100"),
                        10
                )
        ));

        assertEquals(0, new BigDecimal("1100000")
                .compareTo(resultado.totalOperacion()));
        assertEquals(0, new BigDecimal("1000000")
                .compareTo(resultado.baseGravada10()));
        assertEquals(0, new BigDecimal("100000")
                .compareTo(resultado.iva10()));
    }

    @Test
    void exigeTasaCeroParaUnItemExento() {
        ItemDocumentoSifen invalido = new ItemDocumentoSifen(
                "CONS-001", "Consulta médica", BigDecimal.ONE,
                new BigDecimal("100000"), AfectacionIvaSifen.EXENTO,
                BigDecimal.ZERO, 10
        );

        assertThrows(IllegalArgumentException.class, () ->
                servicio.calcular(List.of(invalido))
        );
    }
}
