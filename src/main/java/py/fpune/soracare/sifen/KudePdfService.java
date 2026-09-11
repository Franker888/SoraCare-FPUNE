package py.fpune.soracare.sifen;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.w3c.dom.Element;
import py.fpune.soracare.dto.DocumentoKudeDTO;
import py.fpune.soracare.service.ComprobantePdfService.DocumentoPdf;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/** Representacion del XML persistido. No emite, firma ni transmite documentos. */
public class KudePdfService {
    private static final PDType1Font NORMAL = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font NEGRITA = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final float ANCHO = 499;
    private static final String CONSULTA = "https://ekuatia.set.gov.py/consultas-test/";

    public DocumentoPdf generar(DocumentoKudeDTO datos) {
        if (datos == null || !"PRUEBA".equals(datos.ambiente()) || !datos.xsdValidado()
                || !("VALIDADO".equals(datos.estado()) || "APROBADO".equals(datos.estado()))) {
            throw new IllegalArgumentException("KuDE habilitado solo para DE validados o aprobados en PRUEBA.");
        }
        byte[] xml = datos.xmlFirmado();
        if (xml.length == 0 || xml.length > 2_000_000 || !hash(xml).equals(datos.sha256())) {
            throw new IllegalArgumentException("Integridad del XML incorrecta.");
        }
        new ValidadorXsdSifenService().validar(xml);
        var raiz = XmlSifenUtil.leer(xml).getDocumentElement();
        Element de = hijo(raiz, "DE");
        String cdc = de.getAttribute("Id");
        if (!cdc.matches("[0-9]{44}") || !cdc.equals(datos.cdc())) {
            throw new IllegalArgumentException("CDC inconsistente.");
        }
        String qr = texto(hijo(raiz, "gCamFuFD"), "dCarQR");
        URI uri = URI.create(qr);
        if (!"https".equals(uri.getScheme()) || !"ekuatia.set.gov.py".equals(uri.getHost())
                || !"/consultas-test/qr".equals(uri.getPath()) || uri.getUserInfo() != null
                || uri.getPort() != -1 || uri.getFragment() != null || uri.getRawQuery() == null
                || !List.of(uri.getRawQuery().split("&")).contains("Id=" + cdc)) {
            throw new IllegalArgumentException("QR inconsistente con el ambiente o CDC.");
        }
        Element timbrado = hijo(de, "gTimb"), generales = hijo(de, "gDatGralOpe");
        Element emisor = hijo(generales, "gEmis"), receptor = hijo(generales, "gDatRec");
        Element operacion = hijo(generales, "gOpeCom"), detalle = hijo(de, "gDtipDE");
        // Alcance actual del emisor: factura de servicios al contado en guaranies.
        if (!"1".equals(texto(timbrado, "iTiDE")) || !"PYG".equals(texto(operacion, "cMoneOpe"))
                || !"1".equals(texto(hijo(detalle, "gCamCond"), "iCondOpe"))) {
            throw new IllegalArgumentException("Tipo de documento, moneda o condicion fuera del alcance del KuDE.");
        }
        String numero = texto(timbrado, "dEst") + "-" + texto(timbrado, "dPunExp") + "-" + texto(timbrado, "dNumDoc");
        List<Linea> lineas = new ArrayList<>();
        agregar(lineas, "EMISOR", true);
        agregar(lineas, texto(emisor, "dNomEmi"), true);
        opcional(lineas, emisor, "dNomFanEmi", "Nombre fantasia: ");
        for (Element actividad : hijos(emisor, "gActEco")) agregar(lineas, texto(actividad, "dDesActEco"), false);
        agregar(lineas, "RUC: " + texto(emisor, "dRucEm") + "-" + texto(emisor, "dDVEmi"), false);
        agregar(lineas, "Direccion: " + texto(emisor, "dDirEmi") + " " + texto(emisor, "dNumCas") + ", " + texto(emisor, "dDesCiuEmi"), false);
        agregar(lineas, "Telefono: " + texto(emisor, "dTelEmi") + " | Correo: " + texto(emisor, "dEmailE"), false);
        agregar(lineas, "Timbrado: " + texto(timbrado, "dNumTim") + " | Inicio: " + texto(timbrado, "dFeIniT"), false);
        agregar(lineas, "Emision: " + texto(generales, "dFeEmiDE") + " | Contado | Moneda: PYG", false);
        agregar(lineas, "Transaccion: " + texto(operacion, "dDesTipTra"), false);
        agregar(lineas, "RECEPTOR", true);
        agregar(lineas, texto(receptor, "dNomRec"), false);
        agregar(lineas, "1".equals(texto(receptor, "iNatRec"))
                ? "RUC: " + texto(receptor, "dRucRec") + "-" + texto(receptor, "dDVRec")
                : texto(receptor, "dDTipIDRec") + ": " + texto(receptor, "dNumIDRec"), false);
        opcional(lineas, receptor, "dDirRec", "Direccion: ");
        opcional(lineas, receptor, "dTelRec", "Telefono: ");
        opcional(lineas, receptor, "dEmailRec", "Correo: ");
        agregar(lineas, "DETALLE DE LA OPERACION", true);
        for (Element item : hijos(detalle, "gCamItem")) {
            List<Linea> bloque = new ArrayList<>();
            Element valor = hijo(item, "gValorItem"), resta = hijo(valor, "gValorRestaItem"), iva = hijo(item, "gCamIVA");
            if (!List.of("1", "2", "3").contains(texto(iva, "iAfecIVA"))) {
                throw new IllegalArgumentException("Afectacion mixta fuera del alcance actual.");
            }
            agregar(bloque, texto(item, "dCodInt") + " | " + texto(item, "dDesProSer"), true);
            agregar(bloque, "Unidad: " + texto(item, "dDesUniMed") + " | Cantidad: " + numero(item, "dCantProSer")
                    + " | Precio unitario: " + numero(valor, "dPUniProSer") + " | Descuento unitario: " + numero(resta, "dDescItem"), false);
            agregar(bloque, "Afectacion: " + texto(iva, "dDesAfecIVA") + " | IVA: " + numero(iva, "dTasaIVA")
                    + "% | Importe PYG: " + numero(resta, "dTotOpeItem"), false);
            agregarBloque(lineas, bloque);
        }
        Element totales = hijo(de, "gTotSub");
        List<Linea> resumen = new ArrayList<>();
        agregar(resumen, "TOTALES (PYG)", true);
        agregar(resumen, "Exentas: " + numero(totales, "dSubExe") + " | Exoneradas: " + numero(totales, "dSubExo")
                + " | Gravadas 5%: " + numero(totales, "dSub5") + " | Gravadas 10%: " + numero(totales, "dSub10"), false);
        agregar(resumen, "Total de la operacion: " + numero(totales, "dTotOpe"), false);
        agregar(resumen, "TOTAL EN GUARANIES: " + numero(totales, "dTotGralOpe"), true);
        agregar(resumen, "Liquidacion IVA 5%: " + numero(totales, "dIVA5") + " | IVA 10%: " + numero(totales, "dIVA10")
                + " | Total IVA: " + numero(totales, "dTotIVA"), false);
        agregarBloque(lineas, resumen);
        try {
            byte[] contenido = dibujar(lineas, qr, cdc, numero, datos.estado());
            return new DocumentoPdf("kude-prueba-" + cdc + ".pdf", contenido, hash(contenido));
        } catch (Exception error) {
            throw new IllegalStateException("No fue posible generar el KuDE de prueba.", error);
        }
    }

    private static byte[] dibujar(List<Linea> lineas, String qr, String cdc, String numero, String estado) throws Exception {
        // Bloques de altura fija despues del ajuste por ancho: no se corta texto.
        final int porPagina = 34;
        int paginas = (lineas.size() + porPagina - 1) / porPagina;
        var matriz = new QRCodeWriter().encode(qr, BarcodeFormat.QR_CODE, 480, 480, Map.of(EncodeHintType.MARGIN, 8));
        BufferedImage imagen = new BufferedImage(480, 480, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 480; y++) for (int x = 0; x < 480; x++) imagen.setRGB(x, y, matriz.get(x, y) ? 0 : 0xFFFFFF);
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            var qrPdf = LosslessFactory.createFromImage(documento, imagen);
            for (int p = 0; p < paginas; p++) {
                PDPage pagina = new PDPage(PDRectangle.A4);
                documento.addPage(pagina);
                try (var lienzo = new PDPageContentStream(documento, pagina)) {
                    lienzo.setNonStrokingColor(0.08f, 0.19f, 0.3f);
                    lienzo.addRect(36, 755, 523, 56); lienzo.fill();
                    lienzo.setNonStrokingColor(1f, 1f, 1f);
                    escribir(lienzo, "KuDE de Factura Electronica", 48, 786, 16, true);
                    escribir(lienzo, "N. " + numero + " | Estado registrado: " + estado, 48, 766, 10, false);
                    lienzo.setNonStrokingColor(0.7f, 0.12f, 0.08f);
                    escribir(lienzo, "AMBIENTE DE PRUEBA - SIN VALIDEZ FISCAL", 48, 735, 12, true);
                    lienzo.setNonStrokingColor(0.12f, 0.16f, 0.2f);
                    float y = 712;
                    for (int i = p * porPagina; i < Math.min(lineas.size(), (p + 1) * porPagina); i++) {
                        Linea linea = lineas.get(i);
                        escribir(lienzo, linea.texto(), 48, y, 10, linea.negrita());
                        y -= 15;
                    }
                    lienzo.drawImage(qrPdf, 43, 50, 130, 130);
                    escribir(lienzo, "Consulta del documento en SIFEN (pruebas)", 182, 164, 10, true);
                    escribir(lienzo, CONSULTA, 182, 145, 10, false);
                    escribir(lienzo, "CDC:", 182, 126, 10, true);
                    escribir(lienzo, cdc.replaceAll("(.{4})(?!$)", "$1 "), 182, 109, 10, false);
                    escribir(lienzo, "Representacion del XML firmado guardado.", 182, 88, 10, false);
                    escribir(lienzo, "No acredita homologacion ni autorizacion de produccion.", 182, 72, 9, false);
                    escribir(lienzo, "Pagina " + (p + 1) + " / " + paginas, 465, 30, 9, false);
                }
            }
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    private static void escribir(PDPageContentStream lienzo, String texto, float x, float y, float tamano, boolean negrita) throws IOException {
        lienzo.beginText(); lienzo.setFont(negrita ? NEGRITA : NORMAL, tamano);
        lienzo.newLineAtOffset(x, y); lienzo.showText(texto); lienzo.endText();
    }

    private static void agregarBloque(List<Linea> lineas, List<Linea> bloque) {
        if (bloque.size() <= 34 && lineas.size() % 34 + bloque.size() > 34) {
            while (lineas.size() % 34 != 0) lineas.add(new Linea("", false));
        }
        lineas.addAll(bloque);
    }

    private static void agregar(List<Linea> lineas, String texto, boolean negrita) {
        var fuente = negrita ? NEGRITA : NORMAL;
        StringBuilder actual = new StringBuilder();
        try {
            for (int cp : texto.replaceAll("\\s+", " ").codePoints().toArray()) {
                String caracter = new String(Character.toChars(cp));
                // No sustituir silenciosamente nombres fiscales no representables.
                fuente.encode(caracter);
                if (fuente.getStringWidth(actual + caracter) * 10 / 1000 > ANCHO) {
                    lineas.add(new Linea(actual.toString(), negrita)); actual.setLength(0);
                }
                actual.append(caracter);
            }
            lineas.add(new Linea(actual.toString(), negrita));
        } catch (IOException error) { throw new IllegalArgumentException("Texto no representable en PDF.", error); }
    }

    private static void opcional(List<Linea> lineas, Element elemento, String campo, String prefijo) {
        String valor = texto(elemento, campo);
        if (!valor.isBlank()) agregar(lineas, prefijo + valor, false);
    }
    private static List<Element> hijos(Element padre, String nombre) {
        List<Element> resultado = new ArrayList<>();
        for (var n = padre.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && nombre.equals(e.getLocalName()) && XmlSifenUtil.NAMESPACE.equals(e.getNamespaceURI())) resultado.add(e);
        }
        return resultado;
    }
    private static Element hijo(Element padre, String nombre) {
        var elementos = hijos(padre, nombre);
        if (elementos.size() != 1) throw new IllegalArgumentException("Grupo XML ausente o duplicado: " + nombre);
        return elementos.getFirst();
    }
    private static String texto(Element padre, String nombre) {
        var elementos = hijos(padre, nombre);
        if (elementos.size() > 1) throw new IllegalArgumentException("Campo XML duplicado.");
        return elementos.isEmpty() ? "" : elementos.getFirst().getTextContent();
    }
    private static String numero(Element padre, String nombre) {
        String valor = texto(padre, nombre);
        return valor.isBlank() ? "0" : new BigDecimal(valor).stripTrailingZeros().toPlainString();
    }
    private static String hash(byte[] contenido) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido)); }
        catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
    private record Linea(String texto, boolean negrita) { }
}
