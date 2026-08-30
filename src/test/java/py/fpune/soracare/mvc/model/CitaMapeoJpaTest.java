package py.fpune.soracare.mvc.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CitaMapeoJpaTest {

    @Test
    void citaYEstadoSonEntidadesDeLaUnidadDePersistencia() {
        verificarEntidad(Cita.class, "citas");
        verificarEntidad(EstadoCita.class, "estados_citas");
    }

    @Test
    void estadoSeMapeaComoRelacionDeSoloLectura() throws Exception {
        Field campoEstado = Cita.class.getDeclaredField("estado");
        ManyToOne relacion = campoEstado.getAnnotation(ManyToOne.class);
        JoinColumn columna = campoEstado.getAnnotation(JoinColumn.class);

        assertNotNull(relacion);
        assertEquals(FetchType.LAZY, relacion.fetch());
        assertNotNull(columna);
        assertEquals("id_estado_cita", columna.name());
        assertFalse(columna.insertable());
        assertFalse(columna.updatable());
        assertNotNull(
                Cita.class.getDeclaredField("codigoEstado")
                        .getAnnotation(Transient.class)
        );
    }

    @Test
    void conservaDatosDeEstadoParaLecturasJpaYJdbc() throws Exception {
        Cita citaJdbc = new Cita();
        citaJdbc.setCodigoEstado("CONFIRMADA");
        citaJdbc.setNombreEstado("Confirmada");

        assertEquals("CONFIRMADA", citaJdbc.getCodigoEstado());
        assertEquals("Confirmada", citaJdbc.getNombreEstado());

        EstadoCita estado = new EstadoCita();
        estado.setCodigo("ATENDIDA");
        estado.setNombre("Atendida");

        Cita citaJpa = new Cita();
        Field campoEstado = Cita.class.getDeclaredField("estado");
        campoEstado.setAccessible(true);
        campoEstado.set(citaJpa, estado);

        assertEquals("ATENDIDA", citaJpa.getCodigoEstado());
        assertEquals("Atendida", citaJpa.getNombreEstado());
    }

    private static void verificarEntidad(
            Class<?> tipo,
            String nombreTabla
    ) {
        assertNotNull(tipo.getAnnotation(Entity.class));
        Table tabla = tipo.getAnnotation(Table.class);
        assertNotNull(tabla);
        assertEquals("soracare", tabla.schema());
        assertEquals(nombreTabla, tabla.name());
    }
}
