package py.fpune.soracare.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import py.fpune.soracare.api.controller.ApiRestExcepciones;
import py.fpune.soracare.api.controller.EspecialidadRestController;
import py.fpune.soracare.service.EspecialidadService;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiRestConfiguracionTest {

    @Test
    void registraElDispatcherEnLaRutaVersionada() throws Exception {
        Document documento = cargarWebXml();
        Element servlet = buscarPorNombre(
                documento,
                "servlet",
                "apiRest"
        );
        Element mapeo = buscarPorNombre(
                documento,
                "servlet-mapping",
                "apiRest"
        );

        assertEquals(
                "org.springframework.web.servlet.DispatcherServlet",
                texto(servlet, "servlet-class")
        );
        assertEquals("2", texto(servlet, "load-on-startup"));
        assertEquals("/api/v1/*", texto(mapeo, "url-pattern"));
    }

    @Test
    void creaSolamenteLosComponentesNecesariosParaLaApi() {
        try (AnnotationConfigWebApplicationContext contexto =
                     new AnnotationConfigWebApplicationContext()) {
            contexto.setServletContext(new MockServletContext());
            contexto.register(ApiRestConfiguracion.class);
            contexto.refresh();

            assertNotNull(contexto.getBean(EspecialidadService.class));
            assertNotNull(contexto.getBean(EspecialidadRestController.class));
            assertNotNull(contexto.getBean(ApiRestExcepciones.class));
        }
    }

    private static Document cargarWebXml() throws Exception {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );
        fabrica.setExpandEntityReferences(false);
        return fabrica.newDocumentBuilder().parse(
                Path.of("src/main/webapp/WEB-INF/web.xml").toFile()
        );
    }

    private static Element buscarPorNombre(
            Document documento,
            String etiqueta,
            String nombre
    ) {
        var nodos = documento.getElementsByTagName(etiqueta);

        for (int indice = 0; indice < nodos.getLength(); indice++) {
            Element elemento = (Element) nodos.item(indice);
            if (nombre.equals(texto(elemento, "servlet-name"))) {
                return elemento;
            }
        }

        throw new AssertionError("No se encontró el servlet " + nombre + ".");
    }

    private static String texto(Element elemento, String etiqueta) {
        return elemento.getElementsByTagName(etiqueta)
                .item(0)
                .getTextContent()
                .strip();
    }
}
