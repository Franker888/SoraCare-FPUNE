package py.fpune.soracare.sifen;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;
import static org.junit.jupiter.api.Assertions.*;
import static py.fpune.soracare.sifen.ResultadoConsultaSifen.Estado.*;

class ClienteConsultaSifenServiceTest {
    private static final String CDC = "1".repeat(44);
    private final ClienteConsultaSifenService cliente = new ClienteConsultaSifenService();

    @Test void construyeConsultaConIdYCDCNoIncluyeElDocumento() {
        var xml = XmlSifenUtil.leer(cliente.construirSobre(7, CDC));
        assertEquals("7", xml.getElementsByTagNameNS(XmlSifenUtil.NAMESPACE, "dId").item(0).getTextContent());
        assertEquals(CDC, xml.getElementsByTagNameNS(XmlSifenUtil.NAMESPACE, "dCDC").item(0).getTextContent());
        assertEquals(0, xml.getElementsByTagNameNS(XmlSifenUtil.NAMESPACE, "rDE").getLength());
        assertThrows(IllegalArgumentException.class, () -> cliente.construirSobre(0, CDC));
        assertThrows(IllegalArgumentException.class, () -> cliente.construirSobre(1, "<CDC>"));
        assertThrows(IllegalArgumentException.class, () -> cliente.construirSobre(1_000_000_000_000_000L, CDC));
    }
    @Test void distingueInexistenteSinPermisoYCodigoDesconocido() {
        assertEquals(NO_ENCONTRADO, cliente.interpretar(200, respuesta("0420", ""), CDC).estado());
        assertEquals(SIN_PERMISO, cliente.interpretar(200, respuesta("0421", ""), CDC).estado());
        assertEquals(ERROR, cliente.interpretar(200, respuesta("9999", ""), CDC).estado());
    }
    @Test void confirmaSoloElCdcDevueltoConProtocoloYCuentaEventos() {
        var r = cliente.interpretar(200, respuesta("0422", "<xContenDE>" + contenido(CDC) + "</xContenDE>"), CDC);
        assertEquals(ENCONTRADO, r.estado()); assertEquals("1234567890", r.protocolo()); assertEquals(1, r.eventos());
        assertEquals(ERROR, cliente.interpretar(200, respuesta("0422", "<xContenDE>" + contenido("2".repeat(44)) + "</xContenDE>"), CDC).estado());
        assertEquals(ERROR, cliente.interpretar(200, respuesta("0422", ""), CDC).estado());
    }
    @Test void admiteContenedorXmlEscapadoSinAccederARecursosExternos() {
        String xml = contenido(CDC).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        assertEquals(ENCONTRADO, cliente.interpretar(200, respuesta("0422", "<xContenDE>" + xml + "</xContenDE>"), CDC).estado());
        String malicioso = "<!DOCTYPE rContDe [<!ENTITY x SYSTEM 'file:///no-leer'>]><rContDe>&x;</rContDe>";
        assertEquals(ERROR, cliente.interpretar(200, malicioso.getBytes(StandardCharsets.UTF_8), CDC).estado());
    }
    @Test void rechazaHttpFallidoXmlAmbiguoYContenedorContradictorio() {
        assertEquals(ERROR, cliente.interpretar(500, respuesta("0422", ""), CDC).estado());
        assertEquals(ERROR, cliente.interpretar(200, new byte[0], CDC).estado());
        assertEquals(ERROR, cliente.interpretar(200, respuesta("0420", "<dCodRes>0422</dCodRes>"), CDC).estado());
        assertEquals(ERROR, cliente.interpretar(200, respuesta("0420", "<xContenDE/>"), CDC).estado());
        assertEquals(ERROR, cliente.interpretar(200, new byte[2_000_001], CDC).estado());
        assertEquals(ERROR, cliente.interpretar(200, "<html>0422</html>".getBytes(), CDC).estado());
    }
    @Test void cancelaDescargaAntesDeAcumularUnaRespuestaExcesiva() {
        var cuerpo = ClienteConsultaSifenService.cuerpoLimitado();
        boolean[] cancelada = {false};
        cuerpo.onSubscribe(new Flow.Subscription() {
            public void request(long n) { }
            public void cancel() { cancelada[0] = true; }
        });
        cuerpo.onNext(List.of(ByteBuffer.wrap(new byte[2_000_001])));
        assertTrue(cancelada[0]); assertTrue(cuerpo.getBody().toCompletableFuture().isCompletedExceptionally());
    }
    private static String contenido(String cdc) {
        return "<rContDe xmlns=\"" + XmlSifenUtil.NAMESPACE + "\"><rDE><DE Id=\"" + cdc
                + "\"/></rDE><dProtAut>1234567890</dProtAut><xContEv/></rContDe>";
    }
    private static byte[] respuesta(String codigo, String contenido) {
        return ("<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\"><soap:Body>"
                + "<rResEnviConsDe xmlns=\"" + XmlSifenUtil.NAMESPACE + "\"><dFecProc>2026-09-11T12:00:00</dFecProc>"
                + "<dCodRes>" + codigo + "</dCodRes><dMsgRes>Resultado de prueba</dMsgRes>" + contenido
                + "</rResEnviConsDe></soap:Body></soap:Envelope>").getBytes(StandardCharsets.UTF_8);
    }
}
