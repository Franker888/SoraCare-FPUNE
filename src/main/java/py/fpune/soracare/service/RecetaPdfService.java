package py.fpune.soracare.service;

import py.fpune.soracare.dto.DetalleRecetaDTO;
import py.fpune.soracare.dto.RecetaMedicaDTO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RecetaPdfService {

    private static final ZoneId ZONA = ZoneId.of("America/Asuncion");
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int LINEAS_POR_PAGINA = 48;
    private static final int CARACTERES_POR_LINEA = 88;

    public DocumentoPdf generar(RecetaMedicaDTO receta) {
        if (receta == null) {
            throw new IllegalArgumentException("La indicación médica es obligatoria.");
        }
        List<String> lineas = new ArrayList<>();
        agregar(lineas, "SORACARE-FPUNE");
        agregar(lineas, "INDICACION MEDICA DIGITAL - " + receta.codigo());
        agregar(lineas, "Sistema academico universitario");
        agregar(lineas, "");
        agregar(lineas, "Paciente: " + receta.nombrePaciente());
        agregar(lineas, "Medico: " + receta.nombreMedico());
        agregar(lineas, "Especialidad: " + receta.especialidad());
        agregar(lineas, "Emision: " + receta.fechaEmision()
                .atZoneSameInstant(ZONA).format(FECHA_HORA));
        agregar(lineas, "Valida hasta: "
                + (receta.fechaVencimiento() == null ? "Sin fecha indicada" : receta.fechaVencimiento()));
        agregar(lineas, "Estado: " + receta.estado());
        agregar(lineas, "");
        agregar(lineas, "MEDICAMENTOS E INDICACIONES");

        int numero = 1;
        for (DetalleRecetaDTO detalle : receta.detalles()) {
            agregar(lineas, numero++ + ". " + detalle.nombreComercial()
                    + valorConEspacio(detalle.concentracion())
                    + " (" + detalle.principioActivo() + ")");
            agregar(lineas, "   Dosis: " + detalle.dosis()
                    + " | Frecuencia: " + detalle.frecuencia()
                    + " | Via: " + detalle.viaAdministracion().getNombreVisible());
            agregar(lineas, "   Cantidad: "
                    + detalle.cantidad().stripTrailingZeros().toPlainString()
                    + " " + detalle.unidad()
                    + (detalle.duracionDias() == null
                    ? "" : " | Duracion: " + detalle.duracionDias() + " dias"));
            if (detalle.instrucciones() != null) {
                agregar(lineas, "   Indicaciones: " + detalle.instrucciones());
            }
            agregar(lineas, "");
        }
        if (receta.indicacionesGenerales() != null) {
            agregar(lineas, "INDICACIONES GENERALES");
            agregar(lineas, receta.indicacionesGenerales());
            agregar(lineas, "");
        }
        if (receta.motivoAnulacion() != null) {
            agregar(lineas, "INDICACION ANULADA: " + receta.motivoAnulacion());
        }
        agregar(lineas, "Documento generado por SoraCare-FPUNE.");

        byte[] contenido = crearPdf(paginar(lineas));
        return new DocumentoPdf("indicacion-medica-" + receta.codigo() + ".pdf", contenido);
    }

    private static void agregar(List<String> destino, String texto) {
        String limpio = normalizarAscii(texto == null ? "" : texto);
        if (limpio.isEmpty()) {
            destino.add("");
            return;
        }
        String restante = limpio;
        while (restante.length() > CARACTERES_POR_LINEA) {
            int corte = restante.lastIndexOf(' ', CARACTERES_POR_LINEA);
            if (corte < 20) corte = CARACTERES_POR_LINEA;
            destino.add(restante.substring(0, corte).stripTrailing());
            restante = restante.substring(corte).stripLeading();
        }
        destino.add(restante);
    }

    private static List<List<String>> paginar(List<String> lineas) {
        List<List<String>> paginas = new ArrayList<>();
        for (int inicio = 0; inicio < lineas.size(); inicio += LINEAS_POR_PAGINA) {
            paginas.add(List.copyOf(lineas.subList(
                    inicio, Math.min(inicio + LINEAS_POR_PAGINA, lineas.size()))));
        }
        if (paginas.isEmpty()) paginas.add(List.of("SoraCare-FPUNE"));
        return paginas;
    }

    private static byte[] crearPdf(List<List<String>> paginas) {
        int cantidadPaginas = paginas.size();
        int idFuente = 3 + cantidadPaginas * 2;
        List<byte[]> objetos = new ArrayList<>();
        objetos.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder hijos = new StringBuilder();
        for (int i = 0; i < cantidadPaginas; i++) hijos.append(3 + i * 2).append(" 0 R ");
        objetos.add(bytes("<< /Type /Pages /Kids [" + hijos + "] /Count "
                + cantidadPaginas + " >>"));

        for (int i = 0; i < cantidadPaginas; i++) {
            int idContenido = 4 + i * 2;
            objetos.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 " + idFuente
                    + " 0 R >> >> /Contents " + idContenido + " 0 R >>"));
            byte[] stream = contenidoPagina(paginas.get(i), i + 1, cantidadPaginas);
            objetos.add(concatenar(bytes("<< /Length " + stream.length + " >>\nstream\n"),
                    stream, bytes("endstream")));
        }
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
            throw new IllegalStateException("No fue posible generar el PDF de la indicación médica.", error);
        }
    }

    private static byte[] contenidoPagina(
            List<String> lineas, int pagina, int totalPaginas
    ) {
        StringBuilder contenido = new StringBuilder("BT\n/F1 10 Tf\n45 805 Td\n");
        for (int i = 0; i < lineas.size(); i++) {
            if (i > 0) contenido.append("0 -15 Td\n");
            contenido.append('(').append(escaparPdf(lineas.get(i))).append(") Tj\n");
        }
        contenido.append("0 -25 Td\n(Pagina ").append(pagina).append(" de ")
                .append(totalPaginas).append(") Tj\nET\n");
        return contenido.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String normalizarAscii(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\x20-\\x7E]", "?");
    }

    private static String escaparPdf(String valor) {
        return valor.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
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

    private static String valorConEspacio(String valor) {
        return valor == null || valor.isBlank() ? "" : " " + valor;
    }

    public record DocumentoPdf(String nombreArchivo, byte[] contenido) {
    }
}
