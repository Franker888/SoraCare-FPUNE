package py.fpune.soracare.sifen;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

final class XmlSifenUtil {

    static final String NAMESPACE = "http://ekuatia.set.gov.py/sifen/xsd";

    private XmlSifenUtil() {
    }

    static Document nuevoDocumento() {
        try {
            DocumentBuilderFactory fabrica = fabricaSegura();
            return fabrica.newDocumentBuilder().newDocument();
        } catch (Exception error) {
            throw new IllegalStateException("No fue posible crear el XML SIFEN.", error);
        }
    }

    static Document leer(byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new IllegalArgumentException("El XML SIFEN está vacío.");
        }
        try {
            DocumentBuilderFactory fabrica = fabricaSegura();
            var constructor = fabrica.newDocumentBuilder();
            constructor.setEntityResolver((publicId, systemId) ->
                    new InputSource(new ByteArrayInputStream(new byte[0])));
            return constructor.parse(new ByteArrayInputStream(contenido));
        } catch (Exception error) {
            throw new IllegalArgumentException("El XML SIFEN no es válido.", error);
        }
    }

    static byte[] escribir(Document documento) {
        try {
            TransformerFactory fabrica = TransformerFactory.newInstance();
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformador = fabrica.newTransformer();
            transformador.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            transformador.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformador.setOutputProperty(OutputKeys.INDENT, "no");
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            transformador.transform(new DOMSource(documento), new StreamResult(salida));
            return salida.toByteArray();
        } catch (Exception error) {
            throw new IllegalStateException("No fue posible serializar el XML SIFEN.", error);
        }
    }

    private static DocumentBuilderFactory fabricaSegura() throws Exception {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        fabrica.setFeature("http://xml.org/sax/features/external-general-entities", false);
        fabrica.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        fabrica.setXIncludeAware(false);
        fabrica.setExpandEntityReferences(false);
        return fabrica;
    }
}
