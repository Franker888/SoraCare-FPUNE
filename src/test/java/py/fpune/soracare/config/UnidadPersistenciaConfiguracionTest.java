package py.fpune.soracare.config;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import py.fpune.soracare.mvc.model.Cita;
import py.fpune.soracare.mvc.model.EstadoCita;
import py.fpune.soracare.mvc.model.ExcepcionHorarioMedico;
import py.fpune.soracare.mvc.model.HorarioMedico;
import py.fpune.soracare.mvc.model.Medicamento;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UnidadPersistenciaConfiguracionTest {

    private static final Map<Class<?>, String> ENTIDADES_ESPERADAS = Map.of(
            ExcepcionHorarioMedico.class, "excepciones_horarios_medicos",
            HorarioMedico.class, "horarios_medicos",
            EstadoCita.class, "estados_citas",
            Cita.class, "citas",
            Medicamento.class, "medicamentos"
    );

    @Test
    void registraTodasLasEntidadesDelAlcanceJpa() throws Exception {
        Document documento = cargarConfiguracion();
        var nodos = documento.getElementsByTagName("class");
        Set<String> entidadesRegistradas = new HashSet<>();

        for (int indice = 0; indice < nodos.getLength(); indice++) {
            entidadesRegistradas.add(nodos.item(indice).getTextContent().strip());
        }

        Set<String> nombresEsperados = ENTIDADES_ESPERADAS.keySet()
                .stream()
                .map(Class::getName)
                .collect(Collectors.toSet());

        assertEquals(nombresEsperados, entidadesRegistradas);
    }

    @Test
    void mantieneElMapeoDelAlcanceJpa() {
        ENTIDADES_ESPERADAS.forEach((entidad, nombreTabla) -> {
            assertNotNull(entidad.getAnnotation(Entity.class));

            Table tabla = entidad.getAnnotation(Table.class);
            assertNotNull(tabla);
            assertEquals("soracare", tabla.schema());
            assertEquals(nombreTabla, tabla.name());
        });
    }

    @Test
    void usaLaUnidadLocalYElProveedorHibernateEsperados() throws Exception {
        Document documento = cargarConfiguracion();
        Element unidad = (Element) documento
                .getElementsByTagName("persistence-unit")
                .item(0);

        assertEquals(JpaContexto.UNIDAD_PERSISTENCIA, unidad.getAttribute("name"));
        assertEquals("RESOURCE_LOCAL", unidad.getAttribute("transaction-type"));
        assertEquals(
                "org.hibernate.jpa.HibernatePersistenceProvider",
                documento.getElementsByTagName("provider")
                        .item(0)
                        .getTextContent()
                        .strip()
        );
        assertEquals(
                "true",
                documento.getElementsByTagName("exclude-unlisted-classes")
                        .item(0)
                        .getTextContent()
                        .strip()
        );
    }

    @Test
    void mantieneFlywayComoUnicaFuenteDelEsquema() throws Exception {
        Map<String, String> propiedades = propiedades(cargarConfiguracion());

        assertEquals(
                "py.fpune.soracare.config.ConexionHibernateProvider",
                propiedades.get("hibernate.connection.provider_class")
        );
        assertEquals(
                "none",
                propiedades.get("jakarta.persistence.schema-generation.database.action")
        );
        assertEquals("none", propiedades.get("hibernate.archive.autodetection"));
    }

    private static Document cargarConfiguracion() throws Exception {
        InputStream recurso = Objects.requireNonNull(
                UnidadPersistenciaConfiguracionTest.class
                        .getClassLoader()
                        .getResourceAsStream("META-INF/persistence.xml"),
                "No se encontró META-INF/persistence.xml."
        );

        try (recurso) {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true
            );
            fabrica.setExpandEntityReferences(false);
            return fabrica.newDocumentBuilder().parse(recurso);
        }
    }

    private static Map<String, String> propiedades(Document documento) {
        var nodos = documento.getElementsByTagName("property");
        Map<String, String> propiedades = new HashMap<>();

        for (int indice = 0; indice < nodos.getLength(); indice++) {
            Element propiedad = (Element) nodos.item(indice);
            propiedades.put(
                    propiedad.getAttribute("name"),
                    propiedad.getAttribute("value")
            );
        }

        return propiedades;
    }
}
