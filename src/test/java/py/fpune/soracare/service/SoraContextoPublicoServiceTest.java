package py.fpune.soracare.service;

import org.junit.jupiter.api.Test;
import py.fpune.soracare.mvc.model.Especialidad;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.MedicoEspecialidad;
import py.fpune.soracare.mvc.model.TratamientoMedico;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoraContextoPublicoServiceTest {

    @Test
    void construyeContextoConCatalogoPublicoSinDatosSensibles() throws Exception {
        Especialidad cardiologia = especialidad(7, "Cardiología");
        Medico medico = medico(3L, "Carlos", "Prueba", TratamientoMedico.DR);
        medico.setCedula("9999999");
        medico.setMatriculaProfesional("MAT-SECRETA");

        SoraContextoPublicoService servicio = new SoraContextoPublicoService(
                new MedicoServiceFalso(List.of(medico)),
                new EspecialidadServiceFalso(List.of(cardiologia)),
                new RelacionServiceFalso(List.of(relacion(3L, 7))),
                () -> 1_000L
        );

        SoraContextoPublicoService.ContextoPublico contexto =
                servicio.consultar();

        assertTrue(contexto.texto().contains("Cardiología"));
        assertTrue(contexto.texto().contains("Dr. Carlos Prueba"));
        assertFalse(contexto.texto().contains("9999999"));
        assertFalse(contexto.texto().contains("MAT-SECRETA"));
    }

    @Test
    void reutilizaElCatalogoDuranteLaVentanaDeCache() throws Exception {
        AtomicLong reloj = new AtomicLong(5_000L);
        AtomicInteger consultasMedicos = new AtomicInteger();
        MedicoService medicoService = new MedicoService() {
            @Override
            public List<Medico> listarActivos() {
                consultasMedicos.incrementAndGet();
                return List.of();
            }
        };
        SoraContextoPublicoService servicio = new SoraContextoPublicoService(
                medicoService,
                new EspecialidadServiceFalso(List.of()),
                new RelacionServiceFalso(List.of()),
                reloj::get
        );

        servicio.consultar();
        reloj.addAndGet(60_000L);
        servicio.consultar();

        assertEquals(1, consultasMedicos.get());
    }

    private static Especialidad especialidad(int id, String nombre) {
        Especialidad especialidad = new Especialidad();
        especialidad.setIdEspecialidad(id);
        especialidad.setNombre(nombre);
        return especialidad;
    }

    private static Medico medico(
            long id,
            String nombres,
            String apellidos,
            TratamientoMedico tratamiento
    ) {
        Medico medico = new Medico();
        medico.setIdMedico(id);
        medico.setNombres(nombres);
        medico.setApellidos(apellidos);
        medico.setTratamiento(tratamiento);
        return medico;
    }

    private static MedicoEspecialidad relacion(
            long idMedico,
            int idEspecialidad
    ) {
        return new MedicoEspecialidad(
                idMedico,
                idEspecialidad,
                true,
                null
        );
    }

    private static final class MedicoServiceFalso extends MedicoService {
        private final List<Medico> medicos;

        private MedicoServiceFalso(List<Medico> medicos) {
            this.medicos = medicos;
        }

        @Override
        public List<Medico> listarActivos() {
            return medicos;
        }
    }

    private static final class EspecialidadServiceFalso
            extends EspecialidadService {
        private final List<Especialidad> especialidades;

        private EspecialidadServiceFalso(
                List<Especialidad> especialidades
        ) {
            this.especialidades = especialidades;
        }

        @Override
        public List<Especialidad> listarActivas() {
            return especialidades;
        }
    }

    private static final class RelacionServiceFalso
            extends MedicoEspecialidadService {
        private final List<MedicoEspecialidad> relaciones;

        private RelacionServiceFalso(
                List<MedicoEspecialidad> relaciones
        ) {
            this.relaciones = relaciones;
        }

        @Override
        public List<MedicoEspecialidad> listarActivasPorMedico(
                long idMedico
        ) {
            return relaciones.stream()
                    .filter(relacion -> relacion.getIdMedico() == idMedico)
                    .toList();
        }
    }
}
