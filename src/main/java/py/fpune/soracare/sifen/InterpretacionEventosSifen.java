package py.fpune.soracare.sifen;

import org.w3c.dom.Element;
import java.util.ArrayList;
import java.util.List;

/** Observación diagnóstica del contenedor; no certifica firmas ni vigencia fiscal. */
public final class InterpretacionEventosSifen {
    private InterpretacionEventosSifen() { }

    public static String resumir(Element documento, String cdc) {
        var eventos = hijos(documento, "xContEv");
        if (elementos(documento).stream().anyMatch(e -> "xContEv".equals(e.getLocalName())
                && !XmlSifenUtil.NAMESPACE.equals(e.getNamespaceURI()))) return "REVISION_REQUERIDA";
        if (eventos.isEmpty()) return "SIN_EVENTOS_INFORMADOS";
        int cancelaciones = 0;
        for (var contenedor : eventos) {
            try {
                if (elementos(contenedor).size() != 1) return "REVISION_REQUERIDA";
                Element xmlEvento = unico(unico(contenedor, "rContEv"), "xEvento");
                if (elementos(xmlEvento).size() != 1) return "REVISION_REQUERIDA";
                Element evento = unico(xmlEvento, "rGesEve");
                Element datos = unico(evento, "rEve");
                String id = datos.getAttribute("Id");
                if (!id.matches("[0-9]{1,10}") || !"150".equals(campo(datos, "dVerFor"))
                        || !"1".equals(campo(datos, "dTiGDE"))) return "REVISION_REQUERIDA";
                Element grupo = unico(datos, "gGroupTiEvt");
                Element cancelacion = unico(grupo, "rGeVeCan");
                if (elementos(grupo).size() != 1 || !cdc.equals(campo(cancelacion, "Id"))) return "REVISION_REQUERIDA";
                Element raiz = unico(contenedor, "rContEv");
                // El manual denomina al campo rResEnviEventoDe y a la raíz del schema rRetEnviEventoDe.
                var respuestas = new ArrayList<>(hijos(raiz, "rResEnviEventoDe"));
                respuestas.addAll(hijos(raiz, "rRetEnviEventoDe"));
                if (respuestas.size() != 1) return "REVISION_REQUERIDA";
                Element resultado = unico(respuestas.getFirst(), "gResProcEVe");
                if (!id.equals(campo(resultado, "id"))) return "REVISION_REQUERIDA";
                String estado = campo(resultado, "dEstRes");
                if (!("Aprobado".equals(estado) || "Aprobado con observación".equals(estado))
                        || !campo(resultado, "dProtAut").matches("[0-9]{10}")) return "REVISION_REQUERIDA";
                Element procesamiento = unico(resultado, "gResProc");
                if (!"0600".equals(campo(procesamiento, "dCodRes"))) return "REVISION_REQUERIDA";
                cancelaciones++;
            } catch (IllegalArgumentException e) { return "REVISION_REQUERIDA"; }
        }
        return cancelaciones == 1 ? "CANCELACION_APROBADA_INFORMADA" : "REVISION_REQUERIDA";
    }

    private static List<Element> elementos(Element padre) {
        var lista = new ArrayList<Element>();
        for (var n = padre.getFirstChild(); n != null; n = n.getNextSibling())
            if (n instanceof Element e) lista.add(e);
        return lista;
    }
    private static List<Element> hijos(Element padre, String nombre) {
        return elementos(padre).stream().filter(e -> XmlSifenUtil.NAMESPACE.equals(e.getNamespaceURI())
                && nombre.equals(e.getLocalName())).toList();
    }
    private static Element unico(Element padre, String nombre) {
        var lista = hijos(padre, nombre);
        if (lista.size() != 1) throw new IllegalArgumentException("Evento ambiguo o incompleto");
        return lista.getFirst();
    }
    private static String campo(Element padre, String nombre) { return unico(padre, nombre).getTextContent().strip(); }
}
