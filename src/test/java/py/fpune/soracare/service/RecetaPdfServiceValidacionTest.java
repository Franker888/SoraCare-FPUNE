package py.fpune.soracare.service;

import py.fpune.soracare.dto.DetalleRecetaDTO;
import py.fpune.soracare.dto.RecetaMedicaDTO;
import py.fpune.soracare.mvc.model.ViaAdministracion;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class RecetaPdfServiceValidacionTest {

    private RecetaPdfServiceValidacionTest() {
    }

    public static void main(String[] args) {
        DetalleRecetaDTO detalle = new DetalleRecetaDTO(
                1, 1, "Paracetamol", "Acetaminofén", "Comprimidos",
                "500 mg", ViaAdministracion.ORAL, "1 comprimido",
                "Cada 8 horas", 3, new BigDecimal("9.00"),
                "comprimidos", "Tomar después de comer"
        );
        RecetaMedicaDTO receta = new RecetaMedicaDTO(
                1, 2, 3, 4, "RX-0000000001", OffsetDateTime.now(),
                LocalDate.now().plusDays(10), "Mantener hidratación.",
                "EMITIDA", null, null, "Paciente Prueba",
                "Dr. Médico Prueba", "Clínica médica", List.of(detalle)
        );

        RecetaPdfService.DocumentoPdf documento = new RecetaPdfService().generar(receta);
        String contenido = new String(documento.contenido(), StandardCharsets.ISO_8859_1);

        verificar(contenido.startsWith("%PDF-1.4"), "Falta la cabecera PDF.");
        verificar(contenido.endsWith("%%EOF\n"), "Falta el cierre del PDF.");
        verificar(contenido.contains("RX-0000000001"), "Falta el código de la indicación.");
        verificar(contenido.contains("Paracetamol"), "Falta el medicamento indicado.");
        verificar(documento.nombreArchivo().equals("indicacion-medica-RX-0000000001.pdf"),
                "El nombre del archivo no es el esperado.");

        System.out.println("OK: indicación médica PDF, identificación y contenido verificados.");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}
