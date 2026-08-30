package py.fpune.soracare.mvc.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MedicamentoValidacionTest {

    @Test
    void normalizaLosDatosDelCatalogo() {
        Medicamento medicamento = new Medicamento();
        medicamento.setNombreComercial(" Paracetamol ");
        medicamento.setPrincipioActivo(" Acetaminofén ");
        medicamento.setPresentacion(" Comprimidos ");
        medicamento.setConcentracion(" 500 mg ");
        medicamento.setViaAdministracion(ViaAdministracion.ORAL);

        assertEquals("Paracetamol", medicamento.getNombreComercial());
        assertEquals("500 mg", medicamento.getConcentracion());
    }

    @Test
    void rechazaDatosObligatoriosInvalidos() {
        Medicamento medicamento = new Medicamento();

        assertThrows(
                IllegalArgumentException.class,
                () -> medicamento.setNombreComercial("")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> medicamento.setViaAdministracion(null)
        );
    }

    @Test
    void defineElMapeoJpaDelMedicamento() throws Exception {
        assertNotNull(Medicamento.class.getAnnotation(Entity.class));

        Table tabla = Medicamento.class.getAnnotation(Table.class);
        assertNotNull(tabla);
        assertEquals("soracare", tabla.schema());
        assertEquals("medicamentos", tabla.name());

        Field id = Medicamento.class.getDeclaredField("idMedicamento");
        GeneratedValue generacion = id.getAnnotation(GeneratedValue.class);
        assertNotNull(generacion);
        assertEquals(GenerationType.IDENTITY, generacion.strategy());

        Field via = Medicamento.class.getDeclaredField("viaAdministracion");
        Enumerated enumerado = via.getAnnotation(Enumerated.class);
        assertNotNull(enumerado);
        assertEquals(EnumType.STRING, enumerado.value());
    }

    @Test
    void administraFechasMedianteElCicloDeVidaJpa() throws Exception {
        Medicamento medicamento = new Medicamento();
        Method crear = Medicamento.class.getDeclaredMethod("prepararCreacion");
        crear.setAccessible(true);

        assertNotNull(crear.getAnnotation(PrePersist.class));
        crear.invoke(medicamento);
        assertNotNull(medicamento.getFechaCreacion());
        assertEquals(
                medicamento.getFechaCreacion(),
                medicamento.getFechaActualizacion()
        );

        OffsetDateTime fechaAnterior = medicamento.getFechaActualizacion();
        Method actualizar = Medicamento.class.getDeclaredMethod(
                "prepararActualizacion"
        );
        actualizar.setAccessible(true);

        assertNotNull(actualizar.getAnnotation(PreUpdate.class));
        actualizar.invoke(medicamento);
        assertFalse(medicamento.getFechaActualizacion().isBefore(fechaAnterior));
    }
}
