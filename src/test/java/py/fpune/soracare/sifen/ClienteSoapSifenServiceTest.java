package py.fpune.soracare.sifen;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteSoapSifenServiceTest {

    private final ClienteSoapSifenService cliente = new ClienteSoapSifenService();

    @Test
    void construyeSobreSoap12ConIdentificadorYDocumento() {
        byte[] xml = ("<rDE xmlns=\"" + XmlSifenUtil.NAMESPACE
                + "\"><dVerFor>150</dVerFor></rDE>")
                .getBytes(StandardCharsets.UTF_8);

        byte[] resultado = cliente.construirSobre(123456789012345L, xml);
        var documento = XmlSifenUtil.leer(resultado);

        assertEquals(1, documento.getElementsByTagNameNS(
                "http://www.w3.org/2003/05/soap-envelope", "Envelope"
        ).getLength());
        assertEquals("123456789012345", documento.getElementsByTagNameNS(
                XmlSifenUtil.NAMESPACE, "dId"
        ).item(0).getTextContent());
        assertEquals(1, documento.getElementsByTagNameNS(
                XmlSifenUtil.NAMESPACE, "rDE"
        ).getLength());
    }

    @Test
    void interpretaAprobacionYProtocolo() {
        byte[] respuesta = respuesta(
                "Aprobado", "123456789", "0260", "Documento aprobado"
        );

        RespuestaRecepcionSifen resultado = cliente.interpretar(200, respuesta);

        assertEquals(RespuestaRecepcionSifen.Estado.APROBADO, resultado.estado());
        assertEquals("123456789", resultado.protocolo());
        assertEquals("0260", resultado.codigo());
        assertTrue(resultado.mensaje().contains("Documento aprobado"));
    }

    @Test
    void conservaRechazoSinConfundirloConValidezTributaria() {
        RespuestaRecepcionSifen resultado = cliente.interpretar(
                200,
                respuesta("Rechazado", null, "1001", "RUC inválido")
        );

        assertEquals(RespuestaRecepcionSifen.Estado.RECHAZADO, resultado.estado());
        assertEquals("1001", resultado.codigo());
    }

    @Test
    void trataUnaFallaSoapComoErrorTecnico() {
        byte[] respuesta = """
                <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
                  <soap:Body><soap:Fault><soap:Reason>
                    <soap:Text xml:lang="es">Certificado rechazado</soap:Text>
                  </soap:Reason></soap:Fault></soap:Body>
                </soap:Envelope>
                """.getBytes(StandardCharsets.UTF_8);

        RespuestaRecepcionSifen resultado = cliente.interpretar(500, respuesta);

        assertEquals(RespuestaRecepcionSifen.Estado.ERROR, resultado.estado());
        assertEquals("Certificado rechazado", resultado.mensaje());
    }

    private static byte[] respuesta(
            String estado,
            String protocolo,
            String codigo,
            String mensaje
    ) {
        String protocoloXml = protocolo == null
                ? "" : "<dProtAut>" + protocolo + "</dProtAut>";
        return ("""
                <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
                  <soap:Body>
                    <rRetEnviDe xmlns="http://ekuatia.set.gov.py/sifen/xsd">
                      <rProtDe>
                        <dFecProc>2026-09-01T12:00:00-03:00</dFecProc>
                        <dEstRes>%s</dEstRes>
                        %s
                        <gResProc><dCodRes>%s</dCodRes><dMsgRes>%s</dMsgRes></gResProc>
                      </rProtDe>
                    </rRetEnviDe>
                  </soap:Body>
                </soap:Envelope>
                """).formatted(estado, protocoloXml, codigo, mensaje)
                .getBytes(StandardCharsets.UTF_8);
    }
}
