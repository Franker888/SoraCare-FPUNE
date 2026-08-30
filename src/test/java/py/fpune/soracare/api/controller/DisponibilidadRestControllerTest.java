package py.fpune.soracare.api.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import py.fpune.soracare.dto.SlotDisponibleDTO;
import py.fpune.soracare.service.DisponibilidadService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class DisponibilidadRestControllerTest {

    @Test
    void devuelveTurnosDisponiblesConLosFiltrosSolicitados()
            throws Exception {
        LocalDate[] fechaRecibida = new LocalDate[1];
        Long[] medicoRecibido = new Long[1];
        Integer[] especialidadRecibida = new Integer[1];
        DisponibilidadService servicio = new DisponibilidadService() {
            @Override
            public List<SlotDisponibleDTO> listar(
                    LocalDate fecha,
                    Long idMedico,
                    Integer idEspecialidad
            ) {
                fechaRecibida[0] = fecha;
                medicoRecibido[0] = idMedico;
                especialidadRecibida[0] = idEspecialidad;
                return List.of(slotRegular());
            }
        };
        MockMvc mockMvc = crearMockMvc(servicio);

        MvcResult resultado = mockMvc.perform(
                        get("/disponibilidad")
                                .param("fecha", "2030-01-10")
                                .param("idMedico", "10")
                                .param("idEspecialidad", "1")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andReturn();

        assertEquals(200, resultado.getResponse().getStatus());
        assertEquals(LocalDate.of(2030, 1, 10), fechaRecibida[0]);
        assertEquals(10L, medicoRecibido[0]);
        assertEquals(1, especialidadRecibida[0]);

        JsonArray respuesta = JsonParser.parseString(
                resultado.getResponse().getContentAsString()
        ).getAsJsonArray();
        assertEquals(1, respuesta.size());
        JsonObject turno = respuesta.get(0).getAsJsonObject();
        assertEquals(8, turno.get("idHorario").getAsLong());
        assertFalse(turno.has("idExcepcion"));
        assertEquals(10, turno.get("idMedico").getAsLong());
        assertEquals(1, turno.get("idEspecialidad").getAsInt());
        assertEquals(3, turno.get("idConsultorio").getAsLong());
        assertEquals("2030-01-10", turno.get("fecha").getAsString());
        assertEquals("09:00", turno.get("horaInicio").getAsString());
        assertEquals("09:30", turno.get("horaFin").getAsString());
        assertEquals(false, turno.get("disponibilidadExtra").getAsBoolean());
    }

    @Test
    void aceptaLosFiltrosOpcionalesVacios() throws Exception {
        Long[] medicoRecibido = new Long[1];
        Integer[] especialidadRecibida = new Integer[1];
        DisponibilidadService servicio = new DisponibilidadService() {
            @Override
            public List<SlotDisponibleDTO> listar(
                    LocalDate fecha,
                    Long idMedico,
                    Integer idEspecialidad
            ) {
                medicoRecibido[0] = idMedico;
                especialidadRecibida[0] = idEspecialidad;
                return List.of();
            }
        };

        MvcResult resultado = crearMockMvc(servicio).perform(
                        get("/disponibilidad")
                                .param("fecha", "2030-01-10")
                )
                .andReturn();

        assertEquals(200, resultado.getResponse().getStatus());
        assertNull(medicoRecibido[0]);
        assertNull(especialidadRecibida[0]);
    }

    @Test
    void rechazaParametrosInvalidosConErrorJson() throws Exception {
        MockMvc mockMvc = crearMockMvc(servicioVacio());

        verificarSolicitudInvalida(
                mockMvc.perform(get("/disponibilidad")).andReturn()
        );
        verificarSolicitudInvalida(
                mockMvc.perform(
                        get("/disponibilidad")
                                .param("fecha", "10-01-2030")
                ).andReturn()
        );
        verificarSolicitudInvalida(
                mockMvc.perform(
                        get("/disponibilidad")
                                .param("fecha", "2030-01-10")
                                .param("idMedico", "0")
                ).andReturn()
        );
        verificarSolicitudInvalida(
                mockMvc.perform(
                        get("/disponibilidad")
                                .param("fecha", "2030-01-10")
                                .param("idEspecialidad", "texto")
                ).andReturn()
        );
    }

    @Test
    void respondeErrorSeguroCuandoNoPuedeConsultarPostgresql()
            throws Exception {
        SQLException errorTecnico = new SQLException(
                "Detalle interno que no debe salir en la API"
        );
        DisponibilidadService servicio = new DisponibilidadService() {
            @Override
            public List<SlotDisponibleDTO> listar(
                    LocalDate fecha,
                    Long idMedico,
                    Integer idEspecialidad
            ) throws SQLException {
                throw errorTecnico;
            }
        };

        MvcResult resultado = crearMockMvc(servicio).perform(
                        get("/disponibilidad")
                                .param("fecha", "2030-01-10")
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
    void rechazaOperacionesDeEscritura() throws Exception {
        MvcResult resultado = crearMockMvc(servicioVacio()).perform(
                        post("/disponibilidad")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andReturn();

        assertEquals(405, resultado.getResponse().getStatus());
    }

    private static MockMvc crearMockMvc(DisponibilidadService servicio) {
        return MockMvcBuilders.standaloneSetup(
                        new DisponibilidadRestController(servicio)
                )
                .setControllerAdvice(new ApiRestExcepciones())
                .build();
    }

    private static DisponibilidadService servicioVacio() {
        return new DisponibilidadService() {
            @Override
            public List<SlotDisponibleDTO> listar(
                    LocalDate fecha,
                    Long idMedico,
                    Integer idEspecialidad
            ) {
                return List.of();
            }
        };
    }

    private static SlotDisponibleDTO slotRegular() {
        return new SlotDisponibleDTO(
                8L,
                null,
                10L,
                1,
                3L,
                LocalDate.of(2030, 1, 10),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                false
        );
    }

    private static void verificarSolicitudInvalida(MvcResult resultado)
            throws Exception {
        assertEquals(400, resultado.getResponse().getStatus());
        JsonObject respuesta = JsonParser.parseString(
                resultado.getResponse().getContentAsString()
        ).getAsJsonObject();
        assertEquals(
                "SOLICITUD_INVALIDA",
                respuesta.get("codigo").getAsString()
        );
    }
}
