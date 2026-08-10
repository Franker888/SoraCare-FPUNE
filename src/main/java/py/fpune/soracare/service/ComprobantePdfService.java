package py.fpune.soracare.service;

import py.fpune.soracare.dto.DatosFacturacionDTO;
import py.fpune.soracare.dto.PagoResumenDTO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public class ComprobantePdfService {

    private static final ZoneId ZONA = ZoneId.of("America/Asuncion");
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public DocumentoPdf generar(
            PagoResumenDTO pago,
            DatosFacturacionDTO facturacion,
            String numeroComprobante
    ) {
        if (pago == null || facturacion == null) {
            throw new IllegalArgumentException("Los datos del comprobante son obligatorios.");
        }

        List<String> lineas = List.of(
                "SORACARE-FPUNE",
                "COMPROBANTE DE PAGO SIMULADO",
                "Documento academico - sin validez fiscal",
                "",
                "Numero: " + numeroComprobante,
                "Paciente: " + pago.nombrePaciente(),
                "Documento: " + facturacion.tipoDocumento() + " " + facturacion.numeroDocumento(),
                "Nombre/Razon social: " + facturacion.nombreRazonSocial(),
                "Direccion: " + facturacion.direccion(),
                "Correo: " + facturacion.correo(),
                "",
                "Cita: " + pago.fechaCita().atZoneSameInstant(ZONA).format(FECHA_HORA),
                "Medico: " + pago.nombreMedico(),
                "Especialidad: " + pago.nombreEspecialidad(),
                "Metodo: " + pago.nombreMetodo(),
                "Monto: Gs. " + pago.monto().setScale(0, RoundingMode.HALF_UP).toPlainString(),
                "Referencia: " + valor(pago.referenciaExterna()),
                "",
                "El pago fue procesado por el simulador academico de SoraCare-FPUNE."
        );

        byte[] contenido = crearPdf(lineas);
        String nombreArchivo = "comprobante-" + numeroComprobante + ".pdf";
        return new DocumentoPdf(nombreArchivo, contenido, sha256(contenido));
    }

    private static byte[] crearPdf(List<String> lineas) {
        StringBuilder contenido = new StringBuilder("BT\n/F1 17 Tf\n50 795 Td\n");
        for (int i = 0; i < lineas.size(); i++) {
            if (i == 1) contenido.append("/F1 13 Tf\n");
            if (i == 2) contenido.append("/F1 10 Tf\n");
            if (i > 0) contenido.append("0 -24 Td\n");
            contenido.append('(').append(escaparPdf(normalizarAscii(lineas.get(i))))
                    .append(") Tj\n");
        }
        contenido.append("ET\n");
        byte[] stream = contenido.toString().getBytes(StandardCharsets.ISO_8859_1);

        List<byte[]> objetos = new ArrayList<>();
        objetos.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        objetos.add(bytes("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"));
        objetos.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                + "/Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>"));
        objetos.add(concatenar(bytes("<< /Length " + stream.length + " >>\nstream\n"),
                stream, bytes("endstream")));
        objetos.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));

        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            salida.write(bytes("%PDF-1.4\n%SoraCare\n"));
            List<Integer> offsets = new ArrayList<>();
            for (int i = 0; i < objetos.size(); i++) {
                offsets.add(salida.size());
                salida.write(bytes((i + 1) + " 0 obj\n"));
                salida.write(objetos.get(i));
                salida.write(bytes("\nendobj\n"));
            }
            int inicioXref = salida.size();
            salida.write(bytes("xref\n0 " + (objetos.size() + 1) + "\n"));
            salida.write(bytes("0000000000 65535 f \n"));
            for (Integer offset : offsets) {
                salida.write(bytes(String.format("%010d 00000 n \n", offset)));
            }
            salida.write(bytes("trailer\n<< /Size " + (objetos.size() + 1)
                    + " /Root 1 0 R >>\nstartxref\n" + inicioXref + "\n%%EOF\n"));
            return salida.toByteArray();
        } catch (IOException error) {
            throw new IllegalStateException("No fue posible generar el PDF.", error);
        }
    }

    private static byte[] bytes(String valor) {
        return valor.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static byte[] concatenar(byte[]... partes) {
        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            for (byte[] parte : partes) salida.write(parte);
            return salida.toByteArray();
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static String escaparPdf(String valor) {
        return valor.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private static String normalizarAscii(String valor) {
        if (valor == null) return "";
        return java.text.Normalizer.normalize(valor, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\x20-\\x7E]", "?");
    }

    private static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 no esta disponible.", error);
        }
    }

    private static String valor(String texto) {
        return texto == null || texto.isBlank() ? "Sin referencia" : texto;
    }

    public record DocumentoPdf(String nombreArchivo, byte[] contenido, String sha256) {
    }
}
