package py.fpune.soracare.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResumenFacturacionMedicoDTOTest {

    @Test
    void resumePagosConfirmadosYFacturasEmitidas() {
        ResumenFacturacionMedicoDTO resumen =
                ResumenFacturacionMedicoDTO.desde(List.of(
                        pago(1, new BigDecimal("150000"), 10L),
                        pago(2, new BigDecimal("200000"), null)
                ));

        assertEquals(2, resumen.pagos().size());
        assertEquals(1, resumen.facturasEmitidas());
        assertEquals(1, resumen.pendientesEmision());
        assertEquals(new BigDecimal("350000"), resumen.totalCobrado());
        assertEquals(new BigDecimal("150000"), resumen.totalFacturado());
    }

    private static PagoResumenDTO pago(
            long id,
            BigDecimal monto,
            Long idFactura
    ) {
        return new PagoResumenDTO(
                id, id, 1,
                "QR", "Código QR", "CONFIRMADO", "Confirmado",
                monto, "PYG", "REF-" + id,
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(1),
                "Paciente", "1234567", "Médico", "Cardiología",
                "paciente@example.com", null, null, null,
                idFactura,
                idFactura == null ? null : "FI-2026-00000001",
                idFactura == null ? null : "ENVIADO"
        );
    }
}
