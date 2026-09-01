package py.fpune.soracare.sifen;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

public class QrSifenService {

    private static final String URL_PRUEBA =
            "https://ekuatia.set.gov.py/consultas-test/qr?";
    private static final String URL_PRODUCCION =
            "https://ekuatia.set.gov.py/consultas/qr?";

    public DocumentoConQr agregar(
            byte[] xmlFirmado,
            boolean produccion,
            String identificadorCsc,
            char[] csc
    ) {
        if (identificadorCsc == null || !identificadorCsc.matches("[0-9]{4}")) {
            throw new IllegalArgumentException("El IdCSC debe contener 4 dígitos.");
        }
        if (csc == null || csc.length != 32) {
            throw new IllegalArgumentException("El CSC debe contener 32 caracteres.");
        }
        try {
            Document documento = XmlSifenUtil.leer(xmlFirmado);
            Element raiz = documento.getDocumentElement();
            if (documento.getElementsByTagNameNS(XmlSifenUtil.NAMESPACE, "gCamFuFD")
                    .getLength() != 0) {
                throw new IllegalArgumentException("El XML ya contiene datos de QR.");
            }
            String cdc = texto(documento, XmlSifenUtil.NAMESPACE, "DE", "Id");
            String fecha = contenido(documento, XmlSifenUtil.NAMESPACE, "dFeEmiDE");
            String receptor = identificadorReceptor(documento);
            String total = decimal(contenido(
                    documento, XmlSifenUtil.NAMESPACE, "dTotGralOpe"
            ));
            String totalIva = decimal(contenido(
                    documento, XmlSifenUtil.NAMESPACE, "dTotIVA"
            ));
            int cantidadItems = documento.getElementsByTagNameNS(
                    XmlSifenUtil.NAMESPACE, "gCamItem"
            ).getLength();
            String digest = contenido(
                    documento, XMLSignatureNamespace.NAMESPACE, "DigestValue"
            );

            String parametros = "nVersion=150"
                    + "&Id=" + codificar(cdc)
                    + receptor
                    + "&dFeEmiDE=" + hexadecimal(fecha)
                    + "&dTotGralOpe=" + codificar(total)
                    + "&dTotIVA=" + codificar(totalIva)
                    + "&cItems=" + cantidadItems
                    + "&DigestValue=" + hexadecimal(digest)
                    + "&IdCSC=" + identificadorCsc;
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.update(parametros.getBytes(StandardCharsets.UTF_8));
            ByteBuffer cscCodificado = StandardCharsets.UTF_8.encode(
                    CharBuffer.wrap(csc)
            );
            String hash;
            try {
                sha256.update(cscCodificado);
                hash = HexFormat.of().formatHex(sha256.digest());
            } finally {
                if (cscCodificado.hasArray()) {
                    Arrays.fill(cscCodificado.array(), (byte) 0);
                }
            }
            String url = (produccion ? URL_PRODUCCION : URL_PRUEBA)
                    + parametros + "&cHashQR=" + hash;

            Element grupo = documento.createElementNS(XmlSifenUtil.NAMESPACE, "gCamFuFD");
            Element campo = documento.createElementNS(XmlSifenUtil.NAMESPACE, "dCarQR");
            campo.setTextContent(url);
            grupo.appendChild(campo);
            raiz.appendChild(grupo);
            return new DocumentoConQr(XmlSifenUtil.escribir(documento), url);
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("No fue posible generar el QR SIFEN.", error);
        }
    }

    private static String identificadorReceptor(Document documento) {
        var ruc = documento.getElementsByTagNameNS(XmlSifenUtil.NAMESPACE, "dRucRec");
        if (ruc.getLength() == 1) {
            return "&dRucRec=" + codificar(ruc.item(0).getTextContent());
        }
        return "&dNumIDRec=" + codificar(contenido(
                documento, XmlSifenUtil.NAMESPACE, "dNumIDRec"
        ));
    }

    private static String texto(
            Document documento,
            String namespace,
            String nombre,
            String atributo
    ) {
        var nodos = documento.getElementsByTagNameNS(namespace, nombre);
        if (nodos.getLength() != 1 || !(nodos.item(0) instanceof Element elemento)) {
            throw new IllegalArgumentException("Falta el campo XML " + nombre + ".");
        }
        String valor = elemento.getAttribute(atributo);
        if (valor.isBlank()) {
            throw new IllegalArgumentException("Falta el atributo " + atributo + ".");
        }
        return valor;
    }

    private static String contenido(Document documento, String namespace, String nombre) {
        var nodos = documento.getElementsByTagNameNS(namespace, nombre);
        if (nodos.getLength() != 1) {
            throw new IllegalArgumentException("Falta el campo XML " + nombre + ".");
        }
        return nodos.item(0).getTextContent().strip();
    }

    private static String decimal(String valor) {
        return new BigDecimal(valor).stripTrailingZeros().toPlainString();
    }

    private static String hexadecimal(String valor) {
        return HexFormat.of().formatHex(valor.getBytes(StandardCharsets.UTF_8));
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private static final class XMLSignatureNamespace {
        private static final String NAMESPACE = "http://www.w3.org/2000/09/xmldsig#";
    }

    public record DocumentoConQr(byte[] contenido, String urlQr) {
        public DocumentoConQr {
            contenido = contenido == null ? new byte[0] : contenido.clone();
        }

        @Override
        public byte[] contenido() {
            return contenido.clone();
        }
    }
}
