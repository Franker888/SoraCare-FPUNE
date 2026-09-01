package py.fpune.soracare.sifen;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Cliente SOAP 1.2 para el servicio sincrónico oficial de recepción de DE. */
public class ClienteSoapSifenService {

    private static final String SOAP = "http://www.w3.org/2003/05/soap-envelope";
    private static final URI ENDPOINT_PRUEBA = URI.create(
            "https://sifen-test.set.gov.py/de/ws/sync/recibe.wsdl"
    );
    private static final Duration TIMEOUT = Duration.ofSeconds(40);

    public RespuestaRecepcionSifen enviarPrueba(
            long identificadorEnvio,
            byte[] xmlFirmado,
            Path certificadoPkcs12,
            char[] password
    ) {
        if (identificadorEnvio <= 0 || identificadorEnvio > 999_999_999_999_999L) {
            throw new IllegalArgumentException(
                    "El identificador de envío SIFEN no es válido."
            );
        }
        char[] copiaPassword = password == null ? new char[0] : password.clone();
        try {
            byte[] sobre = construirSobre(identificadorEnvio, xmlFirmado);
            HttpClient cliente = HttpClient.newBuilder()
                    .sslContext(contextoTls(certificadoPkcs12, copiaPassword))
                    .connectTimeout(TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
            HttpRequest solicitud = HttpRequest.newBuilder(ENDPOINT_PRUEBA)
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/soap+xml; charset=UTF-8")
                    .header("Accept", "application/soap+xml, application/xml")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(sobre))
                    .build();
            HttpResponse<byte[]> respuesta = cliente.send(
                    solicitud, HttpResponse.BodyHandlers.ofByteArray()
            );
            return interpretar(respuesta.statusCode(), respuesta.body());
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("El envío a SIFEN fue interrumpido.", error);
        } catch (Exception error) {
            throw new IllegalStateException(
                    "No fue posible conectar con el servicio de pruebas SIFEN.", error
            );
        } finally {
            Arrays.fill(copiaPassword, '\0');
        }
    }

    byte[] construirSobre(long identificadorEnvio, byte[] xmlFirmado) {
        Document documentoFirmado = XmlSifenUtil.leer(xmlFirmado);
        if (!"rDE".equals(documentoFirmado.getDocumentElement().getLocalName())) {
            throw new IllegalArgumentException("El documento a enviar no contiene un rDE.");
        }
        Document sobre = XmlSifenUtil.nuevoDocumento();
        Element envelope = sobre.createElementNS(SOAP, "soap:Envelope");
        sobre.appendChild(envelope);
        envelope.appendChild(sobre.createElementNS(SOAP, "soap:Header"));
        Element body = sobre.createElementNS(SOAP, "soap:Body");
        envelope.appendChild(body);
        Element envio = sobre.createElementNS(XmlSifenUtil.NAMESPACE, "rEnviDe");
        body.appendChild(envio);
        Element id = sobre.createElementNS(XmlSifenUtil.NAMESPACE, "dId");
        id.setTextContent(Long.toString(identificadorEnvio));
        envio.appendChild(id);
        Element xde = sobre.createElementNS(XmlSifenUtil.NAMESPACE, "xDE");
        envio.appendChild(xde);
        xde.appendChild(sobre.importNode(documentoFirmado.getDocumentElement(), true));
        return XmlSifenUtil.escribir(sobre);
    }

    RespuestaRecepcionSifen interpretar(int codigoHttp, byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            return new RespuestaRecepcionSifen(
                    RespuestaRecepcionSifen.Estado.ERROR,
                    codigoHttp,
                    null,
                    Integer.toString(codigoHttp),
                    "SIFEN respondió sin contenido.",
                    contenido
            );
        }
        try {
            Document documento = XmlSifenUtil.leer(contenido);
            String fallo = primero(documento, SOAP, "Text");
            if (fallo != null || codigoHttp < 200 || codigoHttp >= 300) {
                return new RespuestaRecepcionSifen(
                        RespuestaRecepcionSifen.Estado.ERROR,
                        codigoHttp,
                        null,
                        Integer.toString(codigoHttp),
                        limitar(fallo == null ? "Respuesta HTTP no exitosa de SIFEN." : fallo),
                        contenido
                );
            }
            String estado = primero(documento, XmlSifenUtil.NAMESPACE, "dEstRes");
            String protocolo = primero(documento, XmlSifenUtil.NAMESPACE, "dProtAut");
            List<String> codigos = todos(documento, XmlSifenUtil.NAMESPACE, "dCodRes");
            List<String> mensajes = todos(documento, XmlSifenUtil.NAMESPACE, "dMsgRes");
            String codigo = codigos.isEmpty() ? null : codigos.get(0);
            String mensaje = limitar(String.join(" | ", mensajes));
            String normalizado = estado == null
                    ? "" : estado.toUpperCase(Locale.ROOT);
            boolean aprobado = normalizado.contains("APROB")
                    || "0260".equals(codigo) || "0261".equals(codigo);
            return new RespuestaRecepcionSifen(
                    aprobado
                            ? RespuestaRecepcionSifen.Estado.APROBADO
                            : RespuestaRecepcionSifen.Estado.RECHAZADO,
                    codigoHttp,
                    protocolo,
                    codigo,
                    mensaje.isBlank() ? limitar(estado) : mensaje,
                    contenido
            );
        } catch (Exception error) {
            return new RespuestaRecepcionSifen(
                    RespuestaRecepcionSifen.Estado.ERROR,
                    codigoHttp,
                    null,
                    Integer.toString(codigoHttp),
                    "La respuesta de SIFEN no posee un XML SOAP válido.",
                    contenido
            );
        }
    }

    private static SSLContext contextoTls(Path archivo, char[] password)
            throws Exception {
        if (archivo == null || !Files.isRegularFile(archivo)
                || !Files.isReadable(archivo)) {
            throw new IllegalArgumentException(
                    "El certificado PKCS#12 para la autenticación mutua no es accesible."
            );
        }
        KeyStore almacen = KeyStore.getInstance("PKCS12");
        try (InputStream entrada = Files.newInputStream(archivo)) {
            almacen.load(entrada, password);
        }
        KeyManagerFactory claves = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm()
        );
        claves.init(almacen, password);
        SSLContext contexto = SSLContext.getInstance("TLS");
        contexto.init(claves.getKeyManagers(), null, null);
        return contexto;
    }

    private static String primero(Document documento, String namespace, String nombre) {
        var nodos = documento.getElementsByTagNameNS(namespace, nombre);
        return nodos.getLength() == 0 ? null : nodos.item(0).getTextContent().strip();
    }

    private static List<String> todos(
            Document documento,
            String namespace,
            String nombre
    ) {
        var nodos = documento.getElementsByTagNameNS(namespace, nombre);
        List<String> valores = new ArrayList<>();
        for (int indice = 0; indice < nodos.getLength(); indice++) {
            String valor = nodos.item(indice).getTextContent().strip();
            if (!valor.isBlank()) valores.add(valor);
        }
        return valores;
    }

    private static String limitar(String valor) {
        if (valor == null) return "";
        String limpio = valor.strip();
        return limpio.length() <= 1000 ? limpio : limpio.substring(0, 1000);
    }
}
