package py.fpune.soracare.service;

import py.fpune.soracare.dto.DatosFacturacionDTO;
import py.fpune.soracare.dto.PagoResumenDTO;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

public class ComprobantePdfServiceValidacionTest {

    public static void main(String[] args) {
        PagoResumenDTO pago = new PagoResumenDTO(
                25, 10, 8,
                "QR", "Codigo QR", "CONFIRMADO", "Confirmado",
                new BigDecimal("150000.00"), "PYG", "PAGO-25-QR-DEMO",
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(3),
                "Paciente Prueba", "1234567", "Medico Prueba", "Cardiologia",
                "paciente@example.com", null, null, null, null, null, null
        );
        DatosFacturacionDTO facturacion = new DatosFacturacionDTO(
                "CI", "1234567", "Paciente Prueba",
                "Ciudad del Este", "paciente@example.com"
        );

        ComprobantePdfService.DocumentoPdf documento = new ComprobantePdfService()
                .generar(pago, facturacion, "SC-00000025");
        String texto = new String(documento.contenido(), StandardCharsets.ISO_8859_1);

        verificar(texto.startsWith("%PDF-1.4"), "El archivo no posee cabecera PDF.");
        verificar(texto.endsWith("%%EOF\n"), "El archivo no posee cierre PDF.");
        verificar(texto.contains("sin validez fiscal"), "Falta la advertencia academica.");
        verificar(documento.sha256().matches("[a-f0-9]{64}"), "El hash SHA-256 no es valido.");
        verificar(documento.nombreArchivo().endsWith(".pdf"), "El nombre no termina en PDF.");

        System.out.println("OK: comprobante PDF academico validado.");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}
