package py.fpune.soracare.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import py.fpune.soracare.api.controller.ApiRestExcepciones;
import py.fpune.soracare.api.controller.CitaRestController;
import py.fpune.soracare.api.controller.DisponibilidadRestController;
import py.fpune.soracare.api.controller.DisponibilidadMedicoRestController;
import py.fpune.soracare.api.controller.EspecialidadRestController;
import py.fpune.soracare.api.controller.MedicoRestController;
import py.fpune.soracare.api.controller.SesionRestController;
import py.fpune.soracare.service.CitaService;
import py.fpune.soracare.service.DisponibilidadService;
import py.fpune.soracare.service.GestionDisponibilidadMedicoService;
import py.fpune.soracare.service.EspecialidadService;
import py.fpune.soracare.service.MedicoEspecialidadService;
import py.fpune.soracare.service.MedicoService;
import py.fpune.soracare.service.PacienteService;

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
                "servlet-name",
                "apiRest"
        );
        Element mapeo = buscarPorNombre(
                documento,
                "servlet-mapping",
                "servlet-name",
                "apiRest"
        );
        Element filtro = buscarPorNombre(
                documento,
                "filter",
                "filter-name",
                "SeguridadApiRest"
        );
        Element mapeoFiltro = buscarPorNombre(
                documento,
                "filter-mapping",
                "filter-name",
                "SeguridadApiRest"
        );

        assertEquals(
                "org.springframework.web.servlet.DispatcherServlet",
                texto(servlet, "servlet-class")
        );
        assertEquals("2", texto(servlet, "load-on-startup"));
        assertEquals("/api/v1/*", texto(mapeo, "url-pattern"));
        assertEquals(
                "py.fpune.soracare.security.FiltroSeguridadApiRest",
                texto(filtro, "filter-class")
        );
        assertEquals("/api/v1/*", texto(mapeoFiltro, "url-pattern"));
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
            assertNotNull(contexto.getBean(MedicoService.class));
            assertNotNull(contexto.getBean(MedicoEspecialidadService.class));
            assertNotNull(contexto.getBean(MedicoRestController.class));
            assertNotNull(contexto.getBean(DisponibilidadService.class));
            assertNotNull(contexto.getBean(DisponibilidadRestController.class));
            assertNotNull(contexto.getBean(
                    GestionDisponibilidadMedicoService.class
            ));
            assertNotNull(contexto.getBean(
                    DisponibilidadMedicoRestController.class
            ));
            assertNotNull(contexto.getBean(SesionRestController.class));
            assertNotNull(contexto.getBean(CitaService.class));
            assertNotNull(contexto.getBean(PacienteService.class));
            assertNotNull(contexto.getBean(CitaRestController.class));
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
            String etiquetaNombre,
            String nombre
    ) {
        var nodos = documento.getElementsByTagName(etiqueta);

        for (int indice = 0; indice < nodos.getLength(); indice++) {
            Element elemento = (Element) nodos.item(indice);
            if (nombre.equals(texto(elemento, etiquetaNombre))) {
                return elemento;
            }
        }

        throw new AssertionError("No se encontró " + nombre + ".");
    }

    private static String texto(Element elemento, String etiqueta) {
        return elemento.getElementsByTagName(etiqueta)
                .item(0)
                .getTextContent()
                .strip();
    }
}
