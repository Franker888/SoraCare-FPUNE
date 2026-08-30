package py.fpune.soracare.api.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import py.fpune.soracare.mvc.model.Especialidad;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.MedicoEspecialidad;
import py.fpune.soracare.mvc.model.TratamientoMedico;
import py.fpune.soracare.service.EspecialidadService;
import py.fpune.soracare.service.MedicoEspecialidadService;
import py.fpune.soracare.service.MedicoService;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class MedicoRestControllerTest {

    @Test
    void devuelveSolamenteInformacionPublicaDeMedicosActivos()
            throws Exception {
        Medico medico = medicoPublico();
        Especialidad cardiologia = especialidad(1, "Cardiología");
        MedicoEspecialidad relacionPrincipal =
                new MedicoEspecialidad(10L, 1, true, null);
        MedicoEspecialidad relacionSinEspecialidadActiva =
                new MedicoEspecialidad(10L, 99, false, null);

        MockMvc mockMvc = crearMockMvc(
                servicioMedicos(List.of(medico)),
                servicioEspecialidades(List.of(cardiologia)),
                servicioRelaciones(List.of(
                        relacionPrincipal,
                        relacionSinEspecialidadActiva
                ))
        );

        MvcResult resultado = mockMvc.perform(
                        get("/medicos")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andReturn();

        assertEquals(200, resultado.getResponse().getStatus());
        JsonArray respuesta = JsonParser.parseString(
                resultado.getResponse().getContentAsString()
        ).getAsJsonArray();
        assertEquals(1, respuesta.size());

        JsonObject medicoJson = respuesta.get(0).getAsJsonObject();
        assertEquals(10, medicoJson.get("id").getAsLong());
        assertEquals("Dra.", medicoJson.get("tratamiento").getAsString());
        assertEquals("Ana", medicoJson.get("nombres").getAsString());
        assertEquals("Benítez", medicoJson.get("apellidos").getAsString());
        assertEquals("MP-123", medicoJson.get("matriculaProfesional").getAsString());
        assertFalse(medicoJson.has("cedula"));
        assertFalse(medicoJson.has("correo"));
        assertFalse(medicoJson.has("telefono"));
        assertFalse(medicoJson.has("direccion"));
        assertFalse(medicoJson.has("fechaNacimiento"));
        assertFalse(medicoJson.has("estado"));
        assertFalse(medicoJson.has("fotoPerfil"));

        JsonArray especialidades =
                medicoJson.getAsJsonArray("especialidades");
        assertEquals(1, especialidades.size());
        JsonObject especialidad = especialidades.get(0).getAsJsonObject();
        assertEquals(1, especialidad.get("id").getAsInt());
        assertEquals("Cardiología", especialidad.get("nombre").getAsString());
        assertEquals(true, especialidad.get("principal").getAsBoolean());
    }

    @Test
    void respondeErrorSeguroCuandoNoPuedeConsultarLosMedicos()
            throws Exception {
        SQLException errorTecnico = new SQLException(
                "Detalle interno que no debe salir en la API"
        );
        MedicoService medicoService = new MedicoService() {
            @Override
            public List<Medico> listarActivos() throws SQLException {
                throw errorTecnico;
            }
        };
        MockMvc mockMvc = crearMockMvc(
                medicoService,
                servicioEspecialidades(List.of()),
                servicioRelaciones(List.of())
        );

        MvcResult resultado = mockMvc.perform(
                        get("/medicos")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andReturn();

        assertEquals(503, resultado.getResponse().getStatus());
        JsonObject respuesta = JsonParser.parseString(
                resultado.getResponse().getContentAsString()
        ).getAsJsonObject();
        assertEquals(
                "BASE_DATOS_NO_DISPONIBLE",
                respuesta.get("codigo").getAsString()
        );
        assertFalse(
                resultado.getResponse()
                        .getContentAsString()
                        .contains(errorTecnico.getMessage())
        );
    }

    @Test
    void rechazaOperacionesDeEscrituraEnElCatalogoPublico()
            throws Exception {
        MockMvc mockMvc = crearMockMvc(
                servicioMedicos(List.of()),
                servicioEspecialidades(List.of()),
                servicioRelaciones(List.of())
        );

        MvcResult resultado = mockMvc.perform(
                        post("/medicos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andReturn();

        assertEquals(405, resultado.getResponse().getStatus());
    }

    private static MockMvc crearMockMvc(
            MedicoService medicoService,
            EspecialidadService especialidadService,
            MedicoEspecialidadService medicoEspecialidadService
    ) {
        return MockMvcBuilders.standaloneSetup(
                        new MedicoRestController(
                                medicoService,
                                especialidadService,
                                medicoEspecialidadService
                        )
                )
                .setControllerAdvice(new ApiRestExcepciones())
                .build();
    }

    private static MedicoService servicioMedicos(List<Medico> medicos) {
        return new MedicoService() {
            @Override
            public List<Medico> listarActivos() {
                return medicos;
            }
        };
    }

    private static EspecialidadService servicioEspecialidades(
            List<Especialidad> especialidades
    ) {
        return new EspecialidadService() {
            @Override
            public List<Especialidad> listarActivas() {
                return especialidades;
            }
        };
    }

    private static MedicoEspecialidadService servicioRelaciones(
            List<MedicoEspecialidad> relaciones
    ) {
        return new MedicoEspecialidadService() {
            @Override
            public List<MedicoEspecialidad> listarActivasPorMedico(
                    long idMedico
            ) {
                return relaciones;
            }
        };
    }

    private static Medico medicoPublico() {
        Medico medico = new Medico();
        medico.setIdMedico(10L);
        medico.setTratamiento(TratamientoMedico.DRA);
        medico.setNombres("Ana");
        medico.setApellidos("Benítez");
        medico.setMatriculaProfesional("MP-123");
        medico.setBiografia("Especialista en salud cardiovascular.");
        medico.setCedula("1234567");
        medico.setCorreo("ana@example.com");
        medico.setTelefono("0981000000");
        medico.setDireccion("Dirección privada");
        medico.setFotoPerfil("ana.jpg");
        return medico;
    }

    private static Especialidad especialidad(int id, String nombre) {
        Especialidad especialidad = new Especialidad(
                nombre,
                "Descripción pública",
                30
        );
        especialidad.setIdEspecialidad(id);
        return especialidad;
    }
}
