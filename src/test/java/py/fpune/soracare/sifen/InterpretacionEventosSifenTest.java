package py.fpune.soracare.sifen;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InterpretacionEventosSifenTest {
    private static final String CDC = "1".repeat(44);

    @Test void ausenciaNoSeInterpretaComoVigencia() {
        assertEquals("SIN_EVENTOS_INFORMADOS", interpretar(""));
    }
    @Test void cancelacionRequiereRespuestaAprobadaEIdentidadCoincidente() {
        assertEquals("CANCELACION_APROBADA_INFORMADA", interpretar(evento()));
        assertEquals("REVISION_REQUERIDA", interpretar(evento().replace(CDC, "2".repeat(44))));
        assertEquals("REVISION_REQUERIDA", interpretar(evento().replace("<id>123</id>", "<id>124</id>")));
        assertEquals("REVISION_REQUERIDA", interpretar(evento().replace("Aprobado", "Rechazado")));
        assertEquals("REVISION_REQUERIDA", interpretar(evento().replace("0600", "0601")));
    }
    @Test void eventoIncompletoDesconocidoODuplicadoRequiereRevision() {
        assertEquals("REVISION_REQUERIDA", interpretar("<xContEv/>"));
        assertEquals("REVISION_REQUERIDA", interpretar("<xContEv xmlns=\"urn:ajeno\"/>"));
        assertEquals("REVISION_REQUERIDA", interpretar(evento() + evento()));
        assertEquals("REVISION_REQUERIDA", interpretar(evento().replace("<dTiGDE>1</dTiGDE>", "<dTiGDE>99</dTiGDE>")));
        assertEquals("REVISION_REQUERIDA", interpretar(evento().replace("<id>123</id>", "<id>123</id><id>123</id>")));
        assertEquals("REVISION_REQUERIDA", interpretar(evento().replace("<dVerFor>150</dVerFor>", "")));
    }
    private static String interpretar(String eventos) {
        var xml = "<rContDe xmlns=\"" + XmlSifenUtil.NAMESPACE + "\">" + eventos + "</rContDe>";
        return InterpretacionEventosSifen.resumir(XmlSifenUtil.leer(xml.getBytes(StandardCharsets.UTF_8)).getDocumentElement(), CDC);
    }
    private static String evento() {
        return """
                <xContEv><rContEv><xEvento><rGesEve><rEve Id="123">
                <dVerFor>150</dVerFor><dTiGDE>1</dTiGDE>
                <gGroupTiEvt><rGeVeCan><Id>%s</Id></rGeVeCan></gGroupTiEvt>
                </rEve></rGesEve></xEvento><rRetEnviEventoDe><gResProcEVe>
                <dEstRes>Aprobado</dEstRes><dProtAut>1234567890</dProtAut><id>123</id>
                <gResProc><dCodRes>0600</dCodRes></gResProc>
                </gResProcEVe></rRetEnviEventoDe></rContEv></xContEv>
                """.formatted(CDC);
    }
}
