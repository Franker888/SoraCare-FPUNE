package py.fpune.soracare.service;

import org.junit.jupiter.api.Test;
import py.fpune.soracare.dto.PagoResumenDTO;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FacturaInternaCsvServiceTest {

    @Test
    void exportaUtf8YNeutralizaFormulasDeHojaDeCalculo() {
        PagoResumenDTO pago = new PagoResumenDTO(
                25, 10, 8,
                "QR", "Código QR", "CONFIRMADO", "Confirmado",
                new BigDecimal("150000"), "PYG", "PAGO-25",
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(3),
                "=HIPERVINCULO(\"https://malicioso\")", "1234567",
                "Médico Prueba", "Cardiología", "paciente@example.com",
                null, null, null,
                12L, "FI-2026-00000001", "ENVIADO"
        );

        FacturaInternaCsvService.DocumentoCsv documento =
                new FacturaInternaCsvService().generar(List.of(pago));
        String csv = new String(documento.contenido(), StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("\ufeff\"ID pago\""));
        assertTrue(csv.contains("\"'=HIPERVINCULO(\"\"https://malicioso\"\")\""));
        assertTrue(csv.contains("\"FI-2026-00000001\""));
        assertTrue(documento.nombreArchivo().matches(
                "facturacion-medico-\\d{4}-\\d{2}-\\d{2}\\.csv"
        ));
    }
}
