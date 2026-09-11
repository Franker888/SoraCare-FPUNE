package py.fpune.soracare.sifen;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import py.fpune.soracare.dto.DocumentoKudeDTO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;

class KudePdfServiceTest {
    @Test void generaPdfConDatosDelXmlYQrLegible() throws Exception {
        var datos = datos(1);
        var pdf = new KudePdfService().generar(datos);
        Files.createDirectories(Path.of("target", "kude-qa"));
        Files.write(Path.of("target", "kude-qa", "kude-prueba.pdf"), pdf.contenido());
        try (var documento = Loader.loadPDF(pdf.contenido())) {
            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("SIN VALIDEZ FISCAL"));
            assertTrue(texto.contains("Consulta médica general"));
            assertTrue(texto.contains("150000"));
            assertTrue(texto.contains("12558946"));
            assertEquals(1, documento.getNumberOfPages());
            var imagen = new PDFRenderer(documento).renderImageWithDPI(0, 200);
            var fuente = new RGBLuminanceSource(imagen.getWidth(), imagen.getHeight(),
                    imagen.getRGB(0, 0, imagen.getWidth(), imagen.getHeight(), null, 0, imagen.getWidth()));
            String qr = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(fuente)),
                    java.util.Map.of(DecodeHintType.TRY_HARDER, true)).getText();
            assertTrue(qr.startsWith("https://ekuatia.set.gov.py/consultas-test/qr?"));
            assertTrue(qr.contains("Id=" + datos.cdc()));
        }
        // Solo ejemplo sintetico para inspeccion visual; no forma parte del WAR.
        Files.createDirectories(Path.of("target", "kude-qa"));
        Files.write(Path.of("target", "kude-qa", "kude-prueba.pdf"), pdf.contenido());
    }

    @Test void paginaSinPerderItemsYDejaTotalesAlFinal() throws Exception {
        var pdf = new KudePdfService().generar(datos(35));
        try (var documento = Loader.loadPDF(pdf.contenido())) {
            assertTrue(documento.getNumberOfPages() > 1);
            var extractor = new PDFTextStripper();
            String texto = extractor.getText(documento);
            assertEquals(35, texto.split("CONS-001", -1).length - 1);
            extractor.setStartPage(1); extractor.setEndPage(1);
            assertFalse(extractor.getText(documento).contains("TOTALES (PYG)"));
            extractor.setStartPage(documento.getNumberOfPages()); extractor.setEndPage(documento.getNumberOfPages());
            assertTrue(extractor.getText(documento).contains("TOTAL EN GUARANIES: 5250000"));
        }
        Files.createDirectories(Path.of("target", "kude-qa"));
        Files.write(Path.of("target", "kude-qa", "kude-multipagina.pdf"), pdf.contenido());
    }

    @Test void rechazaProduccionEstadosNoHabilitadosYHashAlterado() throws Exception {
        var d = datos(1);
        var servicio = new KudePdfService();
        assertThrows(IllegalArgumentException.class, () -> servicio.generar(new DocumentoKudeDTO("PRODUCCION", "APROBADO", d.cdc(), d.xmlFirmado(), d.sha256(), true)));
        for (String estado : new String[]{"ERROR", "RECHAZADO", "ENVIADO", "PENDIENTE"}) {
            assertThrows(IllegalArgumentException.class, () -> servicio.generar(new DocumentoKudeDTO("PRUEBA", estado, d.cdc(), d.xmlFirmado(), d.sha256(), true)));
        }
        assertThrows(IllegalArgumentException.class, () -> servicio.generar(new DocumentoKudeDTO("PRUEBA", "VALIDADO", d.cdc(), d.xmlFirmado(), "0".repeat(64), true)));
        assertThrows(IllegalArgumentException.class, () -> servicio.generar(new DocumentoKudeDTO("PRUEBA", "VALIDADO", "0".repeat(44), d.xmlFirmado(), d.sha256(), true)));
        assertThrows(IllegalArgumentException.class, () -> servicio.generar(new DocumentoKudeDTO("PRUEBA", "VALIDADO", d.cdc(), d.xmlFirmado(), d.sha256(), false)));
    }

    @Test void rechazaQrExternoYXmlConEntidades() throws Exception {
        var d = datos(1);
        byte[] xml = new String(d.xmlFirmado(), java.nio.charset.StandardCharsets.UTF_8)
                .replace("https://ekuatia.set.gov.py/consultas-test/qr", "https://ejemplo.test/consultas-test/qr")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> new KudePdfService().generar(new DocumentoKudeDTO("PRUEBA", "VALIDADO", d.cdc(), xml, hash(xml), true)));
        byte[] malicioso = "<!DOCTYPE rDE [<!ENTITY x SYSTEM 'file:///no-leer'>]><rDE>&x;</rDE>".getBytes();
        assertThrows(IllegalArgumentException.class, () -> new KudePdfService().generar(new DocumentoKudeDTO("PRUEBA", "VALIDADO", d.cdc(), malicioso, hash(malicioso), true)));
    }

    private static DocumentoKudeDTO datos(int cantidad) throws Exception {
        var base = FlujoXmlSifenTest.documento();
        var datos = new DocumentoElectronicoSifen(base.timbrado(), base.fechaInicioTimbrado(), base.establecimiento(),
                base.puntoExpedicion(), base.numeroDocumento(), base.serie(), base.codigoSeguridad(), base.fechaEmision(),
                base.emisor(), base.receptor(), Collections.nCopies(cantidad, base.items().getFirst()),
                base.tipoPago(), base.descripcionTipoPago(), base.informacionEmisor());
        var xml = new XmlDocumentoSifenService().generar(datos);
        var claves = FlujoXmlSifenTest.claves();
        var firmado = new FirmaXmlSifenService().firmar(xml.contenido(), claves.getPrivate(), FlujoXmlSifenTest.certificado(claves));
        var qr = new QrSifenService().agregar(firmado, false, "0001", "A".repeat(32).toCharArray());
        return new DocumentoKudeDTO("PRUEBA", "VALIDADO", xml.cdc(), qr.contenido(), hash(qr.contenido()), true);
    }
    private static String hash(byte[] datos) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(datos)); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }
}
