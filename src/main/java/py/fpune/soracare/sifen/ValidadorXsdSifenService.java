package py.fpune.soracare.sifen;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;

public class ValidadorXsdSifenService {

    private static final String RAIZ_RECURSOS = "sifen/xsd/v150/";
    private final Schema esquema;

    public ValidadorXsdSifenService() {
        this.esquema = cargarEsquema();
    }

    public void validar(byte[] xml) {
        if (xml == null || xml.length == 0) {
            throw new IllegalArgumentException("El XML SIFEN está vacío.");
        }
        try {
            var validador = esquema.newValidator();
            validador.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validador.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            validador.validate(new StreamSource(new ByteArrayInputStream(xml)));
        } catch (SAXException error) {
            throw new IllegalArgumentException(
                    "El XML no cumple el XSD oficial SIFEN v150: " + error.getMessage(),
                    error
            );
        } catch (Exception error) {
            throw new IllegalStateException("No fue posible validar el XML SIFEN.", error);
        }
    }

    private static Schema cargarEsquema() {
        try {
            SchemaFactory fabrica = SchemaFactory.newInstance(
                    XMLConstants.W3C_XML_SCHEMA_NS_URI
            );
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrica.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            fabrica.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            fabrica.setResourceResolver(new ResolverClasspath());
            var recurso = recurso("siRecepDE_v150.xsd");
            StreamSource fuente = new StreamSource(recurso);
            fuente.setSystemId("classpath:/" + RAIZ_RECURSOS + "siRecepDE_v150.xsd");
            return fabrica.newSchema(fuente);
        } catch (Exception error) {
            throw new IllegalStateException(
                    "No fue posible cargar los XSD oficiales SIFEN v150.", error
            );
        }
    }

    private static InputStream recurso(String nombre) {
        InputStream entrada = ValidadorXsdSifenService.class
                .getClassLoader().getResourceAsStream(RAIZ_RECURSOS + nombre);
        if (entrada == null) {
            throw new IllegalStateException("No se encontró el XSD " + nombre + ".");
        }
        return entrada;
    }

    private static final class ResolverClasspath implements LSResourceResolver {
        @Override
        public LSInput resolveResource(
                String type,
                String namespaceURI,
                String publicId,
                String systemId,
                String baseURI
        ) {
            if (systemId == null || systemId.isBlank()) return null;
            String normalizado = systemId.replace('\\', '/');
            String nombre = normalizado.substring(normalizado.lastIndexOf('/') + 1);
            return new EntradaLs(publicId, systemId, recurso(nombre));
        }
    }

    private static final class EntradaLs implements LSInput {
        private final String publicId;
        private final String systemId;
        private InputStream byteStream;

        private EntradaLs(String publicId, String systemId, InputStream byteStream) {
            this.publicId = publicId;
            this.systemId = systemId;
            this.byteStream = byteStream;
        }

        @Override public Reader getCharacterStream() { return null; }
        @Override public void setCharacterStream(Reader characterStream) { }
        @Override public InputStream getByteStream() { return byteStream; }
        @Override public void setByteStream(InputStream byteStream) { this.byteStream = byteStream; }
        @Override public String getStringData() { return null; }
        @Override public void setStringData(String stringData) { }
        @Override public String getSystemId() { return systemId; }
        @Override public void setSystemId(String systemId) { }
        @Override public String getPublicId() { return publicId; }
        @Override public void setPublicId(String publicId) { }
        @Override public String getBaseURI() { return null; }
        @Override public void setBaseURI(String baseURI) { }
        @Override public String getEncoding() { return "UTF-8"; }
        @Override public void setEncoding(String encoding) { }
        @Override public boolean getCertifiedText() { return false; }
        @Override public void setCertifiedText(boolean certifiedText) { }
    }
}
