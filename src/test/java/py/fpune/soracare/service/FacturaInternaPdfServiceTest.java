package py.fpune.soracare.service;

import org.junit.jupiter.api.Test;
import py.fpune.soracare.dto.DatosFacturacionDTO;
import py.fpune.soracare.dto.PagoResumenDTO;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacturaInternaPdfServiceTest {

    @Test
    void generaDocumentoInternoConAdvertenciaTributariaYHash() {
        PagoResumenDTO pago = new PagoResumenDTO(
                25, 10, 8,
                "QR", "Código QR", "CONFIRMADO", "Confirmado",
                new BigDecimal("150000.00"), "PYG", "PAGO-25-QR-DEMO",
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(3),
                "Paciente Prueba", "1234567", "Médico Prueba", "Cardiología",
                "paciente@example.com", 12L, "SC-00000025", "ENVIADO",
                null, null, null
        );
        DatosFacturacionDTO facturacion = new DatosFacturacionDTO(
                "CI", "1234567", "Paciente Prueba",
                "Ciudad del Este", "paciente@example.com"
        );

        ComprobantePdfService.DocumentoPdf documento =
                new FacturaInternaPdfService().generar(
                        pago, facturacion, "FI-2026-00000001"
                );
        String pdf = new String(
                documento.contenido(), StandardCharsets.ISO_8859_1
        );

        assertTrue(pdf.startsWith("%PDF-1.4"));
        assertTrue(pdf.endsWith("%%EOF\n"));
        assertTrue(pdf.contains("FACTURA INTERNA"));
        assertTrue(pdf.contains("SIN VALIDEZ TRIBUTARIA"));
        assertTrue(pdf.contains("FI-2026-00000001"));
        assertTrue(documento.sha256().matches("[a-f0-9]{64}"));
        assertEquals(
                "factura-interna-FI-2026-00000001.pdf",
                documento.nombreArchivo()
        );
    }
}
