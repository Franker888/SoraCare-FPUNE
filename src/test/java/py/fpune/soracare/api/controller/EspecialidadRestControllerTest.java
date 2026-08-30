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
import py.fpune.soracare.service.EspecialidadService;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class EspecialidadRestControllerTest {

    @Test
    void devuelveEspecialidadesActivasComoJson() throws Exception {
        MockMvc mockMvc = crearMockMvc(servicioConEspecialidades());

        MvcResult resultado = mockMvc.perform(
                        get("/especialidades")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andReturn();

        assertEquals(200, resultado.getResponse().getStatus());
        assertTrue(MediaType.APPLICATION_JSON.isCompatibleWith(
                MediaType.parseMediaType(
                        resultado.getResponse().getContentType()
                )
        ));

        JsonArray respuesta = JsonParser.parseString(
                resultado.getResponse().getContentAsString()
        ).getAsJsonArray();
        assertEquals(2, respuesta.size());

        JsonObject cardiologia = respuesta.get(0).getAsJsonObject();
        assertEquals(1, cardiologia.get("id").getAsInt());
        assertEquals("Cardiología", cardiologia.get("nombre").getAsString());
        assertEquals(30, cardiologia.get("duracionConsultaMinutos").getAsInt());
        assertFalse(cardiologia.has("activo"));
    }

    @Test
    void respondeErrorSeguroCuandoPostgresqlNoEstaDisponible()
            throws Exception {
        SQLException errorTecnico = new SQLException(
                "Detalle interno que no debe salir en la API"
        );
        EspecialidadService servicio = new EspecialidadService() {
            @Override
            public List<Especialidad> listarActivas() throws SQLException {
                throw errorTecnico;
            }
        };
        MockMvc mockMvc = crearMockMvc(servicio);

        MvcResult resultado = mockMvc.perform(
                        get("/especialidades")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andReturn();

        assertEquals(503, resultado.getResponse().getStatus());
        JsonObject respuesta = JsonParser.parseString(
                resultado.getResponse().getContentAsString()
        ).getAsJsonObject();
        assertEquals(503, respuesta.get("estado").getAsInt());
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
        MockMvc mockMvc = crearMockMvc(servicioConEspecialidades());

        MvcResult resultado = mockMvc.perform(
                        post("/especialidades")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andReturn();

        assertEquals(405, resultado.getResponse().getStatus());
    }

    private static MockMvc crearMockMvc(EspecialidadService servicio) {
        return MockMvcBuilders.standaloneSetup(
                        new EspecialidadRestController(servicio)
                )
                .setControllerAdvice(new ApiRestExcepciones())
                .build();
    }

    private static EspecialidadService servicioConEspecialidades() {
        List<Especialidad> especialidades = List.of(
                especialidad(1, "Cardiología", "Corazón y circulación", 30),
                especialidad(2, "Pediatría", "Atención infantil", 40)
        );

        return new EspecialidadService() {
            @Override
            public List<Especialidad> listarActivas() {
                return especialidades;
            }
        };
    }

    private static Especialidad especialidad(
            int id,
            String nombre,
            String descripcion,
            int duracion
    ) {
        Especialidad especialidad = new Especialidad(
                nombre,
                descripcion,
                duracion
        );
        especialidad.setIdEspecialidad(id);
        return especialidad;
    }
}
