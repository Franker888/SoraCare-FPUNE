package py.fpune.soracare.api.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.model.Cita;
import py.fpune.soracare.mvc.model.Paciente;
import py.fpune.soracare.service.CitaService;
import py.fpune.soracare.service.PacienteService;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class CitaRestControllerTest {

    @Test
    void listaSolamenteLasCitasDelPacienteAutenticado() throws Exception {
        Paciente paciente = new Paciente();
        paciente.setIdPaciente(22L);
        PacienteServicePrueba pacientes = new PacienteServicePrueba(paciente);
        CitaServicePrueba citas = new CitaServicePrueba(List.of(cita()));

        MvcResult resultado = crearMockMvc(citas, pacientes).perform(
                        get("/citas").session(sesion(usuario("PACIENTE")))
                )
                .andReturn();

        assertEquals(200, resultado.getResponse().getStatus());
        assertEquals(7L, pacientes.ultimaPersonaConsultada);
        assertEquals(22L, citas.ultimoPacienteConsultado);

        JsonArray respuesta = JsonParser.parseString(
                resultado.getResponse().getContentAsString()
        ).getAsJsonArray();
        JsonObject primera = respuesta.get(0).getAsJsonObject();
        assertEquals(41L, primera.get("idCita").getAsLong());
        assertEquals("CONFIRMADA", primera.get("codigoEstado").getAsString());
        assertFalse(primera.has("idPaciente"));
        assertFalse(primera.has("idUsuarioCreador"));
    }

    @Test
    void rechazaLaConsultaSinSesion() throws Exception {
        MvcResult resultado = crearMockMvc(
                new CitaServicePrueba(List.of()),
                new PacienteServicePrueba(null)
        ).perform(get("/citas")).andReturn();

        assertEquals(401, resultado.getResponse().getStatus());
        verificarCodigo(resultado, "NO_AUTENTICADO");
    }

    @Test
    void rechazaLaConsultaSinRolPaciente() throws Exception {
        MvcResult resultado = crearMockMvc(
                new CitaServicePrueba(List.of()),
                new PacienteServicePrueba(null)
        ).perform(get("/citas").session(sesion(usuario("MEDICO"))))
                .andReturn();

        assertEquals(403, resultado.getResponse().getStatus());
        verificarCodigo(resultado, "ACCESO_DENEGADO");
    }

    @Test
    void rechazaUnaCuentaSinFichaPaciente() throws Exception {
        MvcResult resultado = crearMockMvc(
                new CitaServicePrueba(List.of()),
                new PacienteServicePrueba(null)
        ).perform(get("/citas").session(sesion(usuario("PACIENTE"))))
                .andReturn();

        assertEquals(403, resultado.getResponse().getStatus());
        verificarCodigo(resultado, "ACCESO_DENEGADO");
    }

    private static MockMvc crearMockMvc(
            CitaService citaService,
            PacienteService pacienteService
    ) {
        return MockMvcBuilders.standaloneSetup(
                        new CitaRestController(citaService, pacienteService)
                )
                .setControllerAdvice(new ApiRestExcepciones())
                .build();
    }

    private static MockHttpSession sesion(UsuarioAutenticadoDTO usuario) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute("usuarioAutenticado", usuario);
        return sesion;
    }

    private static UsuarioAutenticadoDTO usuario(String rol) {
        return new UsuarioAutenticadoDTO(
                5L,
                7L,
                "paciente.prueba",
                false,
                Set.of(rol)
        );
    }

    private static Cita cita() {
        Cita cita = new Cita();
        cita.setIdCita(41L);
        cita.setIdPaciente(22L);
        cita.setIdMedico(9L);
        cita.setIdEspecialidad(3);
        cita.setIdConsultorio(4L);
        cita.setFechaHoraInicio(
                OffsetDateTime.parse("2026-09-04T16:00:00-03:00")
        );
        cita.setFechaHoraFin(
                OffsetDateTime.parse("2026-09-04T16:30:00-03:00")
        );
        cita.setCodigoEstado("CONFIRMADA");
        cita.setNombreEstado("Confirmada");
        cita.setMotivoConsulta("Control general");
        cita.setObservaciones("Primera consulta REST");
        return cita;
    }

    private static void verificarCodigo(
            MvcResult resultado,
            String codigo
    ) throws Exception {
        JsonObject respuesta = JsonParser.parseString(
                resultado.getResponse().getContentAsString()
        ).getAsJsonObject();
        assertEquals(codigo, respuesta.get("codigo").getAsString());
    }

    private static final class PacienteServicePrueba
            extends PacienteService {

        private final Paciente paciente;
        private long ultimaPersonaConsultada;

        private PacienteServicePrueba(Paciente paciente) {
            this.paciente = paciente;
        }

        @Override
        public Optional<Paciente> buscarPorIdPersona(long idPersona)
                throws SQLException {
            ultimaPersonaConsultada = idPersona;
            return Optional.ofNullable(paciente);
        }
    }

    private static final class CitaServicePrueba extends CitaService {

        private final List<Cita> citas;
        private long ultimoPacienteConsultado;

        private CitaServicePrueba(List<Cita> citas) {
            this.citas = citas;
        }

        @Override
        public List<Cita> listarPorPaciente(long idPaciente)
                throws SQLException {
            ultimoPacienteConsultado = idPaciente;
            return citas;
        }
    }
}
