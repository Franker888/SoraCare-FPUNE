package py.fpune.soracare.sifen;

import org.w3c.dom.Element;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static py.fpune.soracare.sifen.ResultadoConsultaSifen.Estado.*;

public class ClienteConsultaSifenService {
    private static final String SOAP = "http://www.w3.org/2003/05/soap-envelope";
    private static final URI ENDPOINT = URI.create("https://sifen-test.set.gov.py/de/ws/consultas/consulta.wsdl");
    private static final int MAXIMO_RESPUESTA = 2_000_000;

    public ResultadoConsultaSifen consultarPrueba(long identificador, String cdc, Path certificado, char[] password) {
        byte[] sobre = construirSobre(identificador, cdc);
        char[] copia = password == null ? new char[0] : password.clone();
        try {
            var cliente = HttpClient.newBuilder()
                    .sslContext(ClienteSoapSifenService.contextoTls(certificado, copia))
                    .connectTimeout(Duration.ofSeconds(15))
                    .followRedirects(HttpClient.Redirect.NEVER).build();
            var solicitud = HttpRequest.newBuilder(ENDPOINT).timeout(Duration.ofSeconds(40))
                    .header("Content-Type", "application/soap+xml; charset=UTF-8")
                    .header("Accept", "application/soap+xml, application/xml")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(sobre)).build();
            var respuesta = cliente.send(solicitud, info -> cuerpoLimitado());
            return interpretar(respuesta.statusCode(), respuesta.body(), cdc);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return error(cdc, "TRANSPORTE", "La consulta fue interrumpida; el estado local no cambió.");
        } catch (Exception error) {
            return error(cdc, "TRANSPORTE", "No fue posible consultar SIFEN; el estado local no cambió.");
        } finally { Arrays.fill(copia, '\0'); }
    }

    byte[] construirSobre(long identificador, String cdc) {
        if (identificador <= 0 || identificador > 999_999_999_999_999L
                || cdc == null || !cdc.matches("[0-9]{44}")) {
            throw new IllegalArgumentException("Identificador o CDC de consulta inválido.");
        }
        var documento = XmlSifenUtil.nuevoDocumento();
        Element raiz = documento.createElementNS(SOAP, "soap:Envelope"); documento.appendChild(raiz);
        Element cuerpo = documento.createElementNS(SOAP, "soap:Body"); raiz.appendChild(cuerpo);
        Element consulta = documento.createElementNS(XmlSifenUtil.NAMESPACE, "rEnviConsDe"); cuerpo.appendChild(consulta);
        Element id = documento.createElementNS(XmlSifenUtil.NAMESPACE, "dId");
        id.setTextContent(Long.toString(identificador)); consulta.appendChild(id);
        Element codigo = documento.createElementNS(XmlSifenUtil.NAMESPACE, "dCDC");
        codigo.setTextContent(cdc); consulta.appendChild(codigo);
        return XmlSifenUtil.escribir(documento);
    }

    static HttpResponse.BodySubscriber<byte[]> cuerpoLimitado() {
        var destino = HttpResponse.BodySubscribers.ofByteArray();
        return new HttpResponse.BodySubscriber<>() {
            private java.util.concurrent.Flow.Subscription suscripcion;
            private long bytes;
            private boolean terminado;
            @Override public java.util.concurrent.CompletionStage<byte[]> getBody() { return destino.getBody(); }
            @Override public void onSubscribe(java.util.concurrent.Flow.Subscription s) {
                suscripcion = s; destino.onSubscribe(s);
            }
            @Override public void onNext(List<java.nio.ByteBuffer> bloques) {
                if (terminado) return;
                for (var bloque : bloques) bytes += bloque.remaining();
                if (bytes > MAXIMO_RESPUESTA) {
                    terminado = true; suscripcion.cancel();
                    destino.onError(new IllegalArgumentException("Respuesta SIFEN demasiado grande."));
                } else destino.onNext(bloques);
            }
            @Override public void onError(Throwable error) { if (!terminado) { terminado = true; destino.onError(error); } }
            @Override public void onComplete() { if (!terminado) { terminado = true; destino.onComplete(); } }
        };
    }

    ResultadoConsultaSifen interpretar(int http, byte[] xml, String cdc) {
        if (http != 200 || xml == null || xml.length == 0 || xml.length > MAXIMO_RESPUESTA) {
            return error(cdc, Integer.toString(http), "Respuesta de consulta no disponible o fuera de límite.");
        }
        try {
            Element envelope = XmlSifenUtil.leer(xml).getDocumentElement();
            if (!es(envelope, SOAP, "Envelope")) throw new IllegalArgumentException();
            Element body = unico(envelope, SOAP, "Body");
            if (!hijos(body, SOAP, "Fault").isEmpty()) throw new IllegalArgumentException();
            Element respuesta = unico(body, XmlSifenUtil.NAMESPACE, "rResEnviConsDe");
            String codigo = campo(respuesta, "dCodRes"), mensaje = campo(respuesta, "dMsgRes");
            if (!codigo.matches("[0-9]{4}") || mensaje.isBlank() || mensaje.length() > 255) throw new IllegalArgumentException();
            if ("0420".equals(codigo) || "0421".equals(codigo)) {
                if (!hijos(respuesta, XmlSifenUtil.NAMESPACE, "xContenDE").isEmpty()) throw new IllegalArgumentException();
                return new ResultadoConsultaSifen("0420".equals(codigo) ? NO_ENCONTRADO : SIN_PERMISO, cdc, codigo, mensaje, null, 0);
            }
            if (!"0422".equals(codigo)) return error(cdc, codigo, mensaje);
            Element contenedor = unico(respuesta, XmlSifenUtil.NAMESPACE, "xContenDE");
            Element contenido;
            if (hijos(contenedor, XmlSifenUtil.NAMESPACE, "rContDe").isEmpty()) {
                contenido = XmlSifenUtil.leer(contenedor.getTextContent().strip().getBytes(StandardCharsets.UTF_8)).getDocumentElement();
                if (!es(contenido, XmlSifenUtil.NAMESPACE, "rContDe")) throw new IllegalArgumentException();
            } else contenido = unico(contenedor, XmlSifenUtil.NAMESPACE, "rContDe");
            Element documento = unico(contenido, XmlSifenUtil.NAMESPACE, "rDE");
            Element de = unico(documento, XmlSifenUtil.NAMESPACE, "DE");
            if (!cdc.equals(de.getAttribute("Id"))) throw new IllegalArgumentException();
            String protocolo = campo(contenido, "dProtAut");
            if (!protocolo.matches("[0-9]{1,80}")) throw new IllegalArgumentException();
            int eventos = hijos(contenido, XmlSifenUtil.NAMESPACE, "xContEv").size();
            return new ResultadoConsultaSifen(ENCONTRADO, cdc, codigo, mensaje, protocolo, eventos,
                    InterpretacionEventosSifen.resumir(contenido, cdc));
        } catch (Exception error) {
            return error(cdc, "RESPUESTA_INVALIDA", "La respuesta no permite confirmar el CDC consultado.");
        }
    }

    private static ResultadoConsultaSifen error(String cdc, String codigo, String mensaje) {
        return new ResultadoConsultaSifen(ERROR, cdc, codigo, mensaje, null, 0);
    }
    private static boolean es(Element e, String ns, String nombre) {
        return ns.equals(e.getNamespaceURI()) && nombre.equals(e.getLocalName());
    }
    private static List<Element> hijos(Element padre, String ns, String nombre) {
        List<Element> resultado = new ArrayList<>();
        for (var n = padre.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && es(e, ns, nombre)) resultado.add(e);
        }
        return resultado;
    }
    private static Element unico(Element padre, String ns, String nombre) {
        var elementos = hijos(padre, ns, nombre);
        if (elementos.size() != 1) throw new IllegalArgumentException("Respuesta ambigua o incompleta.");
        return elementos.getFirst();
    }
    private static String campo(Element padre, String nombre) {
        return unico(padre, XmlSifenUtil.NAMESPACE, nombre).getTextContent().strip();
    }
}
