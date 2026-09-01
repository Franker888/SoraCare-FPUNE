package py.fpune.soracare.sifen;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CdcSifenServiceTest {

    private final CdcSifenService servicio = new CdcSifenService();

    @Test
    void generaCdcDe44DigitosConVectorPublicado() {
        String cdc = servicio.generar(new CdcSifenService.DatosCdc(
                1,
                "80069563",
                1,
                "001",
                "001",
                "0000006",
                1,
                LocalDate.of(2021, 11, 29),
                1,
                "759571469"
        ));

        assertEquals("01800695631001001000000612021112917595714694", cdc);
        assertEquals(44, cdc.length());
    }

    @Test
    void rechazaCamposQueDesbordanSuLongitud() {
        assertThrows(IllegalArgumentException.class, () ->
                servicio.generar(new CdcSifenService.DatosCdc(
                        1, "123456789", 1, "001", "001", "1", 1,
                        LocalDate.of(2026, 9, 1), 1, "123456789"
                ))
        );
    }
}
