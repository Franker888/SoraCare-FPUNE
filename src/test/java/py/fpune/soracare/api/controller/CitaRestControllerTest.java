package py.fpune.soracare.api.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import py.fpune.soracare.dto.PagoResumenDTO;
import py.fpune.soracare.dto.ResultadoReservaDTO;
import py.fpune.soracare.dto.SolicitudCitaDTO;
import py.fpune.soracare.dto.SolicitudPagoDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.model.Cita;
import py.fpune.soracare.mvc.model.Paciente;
import py.fpune.soracare.service.CitaService;
import py.fpune.soracare.service.PacienteService;
import py.fpune.soracare.service.ReservaPagoService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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

    @Test
    void creaLaCitaParaElPacienteAutenticadoSinAceptarOtraIdentidad()
            throws Exception {
        Paciente paciente = new Paciente();
        paciente.setIdPaciente(22L);
        PacienteServicePrueba pacientes = new PacienteServicePrueba(paciente);
        ReservaPagoServicePrueba reservas = new ReservaPagoServicePrueba(
                new ResultadoReservaDTO(cita(), pago())
        );

        MvcResult resultado = crearMockMvc(
                new CitaServicePrueba(List.of()),
                pacientes,
                reservas
        ).perform(post("/citas")
                        .session(sesion(usuario("PACIENTE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idPaciente": "999",
                                  "idHorario": "12",
                                  "idExcepcion": "",
                                  "idMedico": "9",
                                  "idEspecialidad": "3",
                                  "idConsultorio": "4",
                                  "fecha": "2026-09-04",
                                  "horaInicio": "16:00",
                                  "motivoConsulta": "Control general",
                                  "observaciones": "Reserva desde la API",
                                  "metodoPago": "EFECTIVO",
                                  "datosFacturacion": {
                                    "tipoDocumento": "CI",
                                    "numeroDocumento": "1234567",
                                    "nombreRazonSocial": "Paciente Prueba",
                                    "direccion": "Ciudad del Este",
                                    "correo": "paciente@example.com"
                                  }
                                }
                                """))
                .andReturn();

        assertEquals(201, resultado.getResponse().getStatus());
        assertEquals("22", reservas.solicitudCita.idPaciente());
        assertEquals("WEB", reservas.solicitudCita.canalReserva());
        assertEquals(5L, reservas.idUsuario);

        JsonObject respuesta = JsonParser.parseString(
                resultado.getResponse().getContentAsString()
        ).getAsJsonObject();
        assertEquals(41L, respuesta.getAsJsonObject("cita")
                .get("idCita").getAsLong());
        JsonObject pago = respuesta.getAsJsonObject("pago");
        assertEquals(51L, pago.get("idPago").getAsLong());
        assertFalse(pago.has("idPaciente"));
        assertFalse(pago.has("cedulaPaciente"));
        assertFalse(pago.has("correoFacturacion"));
        assertFalse(pago.has("referenciaExterna"));
    }

    @Test
    void cancelaSolamenteConLaIdentidadDelPacienteAutenticado()
            throws Exception {
        Paciente paciente = new Paciente();
        paciente.setIdPaciente(22L);
        CitaServicePrueba citas = new CitaServicePrueba(List.of());

        MvcResult resultado = crearMockMvc(
                citas,
                new PacienteServicePrueba(paciente)
        ).perform(patch("/citas/41/cancelacion")
                        .session(sesion(usuario("PACIENTE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"No podré asistir\"}"))
                .andReturn();

        assertEquals(204, resultado.getResponse().getStatus());
        assertEquals(41L, citas.ultimaCitaCancelada);
        assertEquals(22L, citas.ultimoPacienteCancelacion);
        assertEquals(5L, citas.ultimoUsuarioCancelacion);
        assertEquals("No podré asistir", citas.ultimoMotivoCancelacion);
    }

    @Test
    void respondeCuatrocientosAnteJsonMalformado() throws Exception {
        MvcResult resultado = crearMockMvc(
                new CitaServicePrueba(List.of()),
                new PacienteServicePrueba(null)
        ).perform(post("/citas")
                        .session(sesion(usuario("PACIENTE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":"))
                .andReturn();

        assertEquals(400, resultado.getResponse().getStatus());
        verificarCodigo(resultado, "SOLICITUD_INVALIDA");
    }

    private static MockMvc crearMockMvc(
            CitaService citaService,
            PacienteService pacienteService
    ) {
        return crearMockMvc(
                citaService,
                pacienteService,
                new ReservaPagoServicePrueba(null)
        );
    }

    private static MockMvc crearMockMvc(
            CitaService citaService,
            PacienteService pacienteService,
            ReservaPagoService reservaPagoService
    ) {
        return MockMvcBuilders.standaloneSetup(
                        new CitaRestController(
                                citaService,
                                pacienteService,
                                reservaPagoService
                        )
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

    private static PagoResumenDTO pago() {
        return new PagoResumenDTO(
                51L,
                41L,
                22L,
                "EFECTIVO",
                "Efectivo",
                "PENDIENTE",
                "Pendiente",
                new BigDecimal("150000"),
                "PYG",
                "REFERENCIA-PRIVADA",
                null,
                OffsetDateTime.parse("2026-09-04T16:00:00-03:00"),
                "Paciente Prueba",
                "1234567",
                "Dra. Prueba",
                "Clínica Médica",
                "paciente@example.com",
                null,
                null,
                null,
                null,
                null,
                null
        );
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
        private long ultimaCitaCancelada;
        private long ultimoPacienteCancelacion;
        private long ultimoUsuarioCancelacion;
        private String ultimoMotivoCancelacion;

        private CitaServicePrueba(List<Cita> citas) {
            this.citas = citas;
        }

        @Override
        public List<Cita> listarPorPaciente(long idPaciente)
                throws SQLException {
            ultimoPacienteConsultado = idPaciente;
            return citas;
        }

        @Override
        public void cancelarPorPaciente(
                long idCita,
                long idPaciente,
                long idUsuario,
                String motivo
        ) {
            ultimaCitaCancelada = idCita;
            ultimoPacienteCancelacion = idPaciente;
            ultimoUsuarioCancelacion = idUsuario;
            ultimoMotivoCancelacion = motivo;
        }
    }

    private static final class ReservaPagoServicePrueba
            extends ReservaPagoService {

        private final ResultadoReservaDTO resultado;
        private SolicitudCitaDTO solicitudCita;
        private SolicitudPagoDTO solicitudPago;
        private long idUsuario;

        private ReservaPagoServicePrueba(ResultadoReservaDTO resultado) {
            this.resultado = resultado;
        }

        @Override
        public ResultadoReservaDTO reservar(
                SolicitudCitaDTO solicitudCita,
                SolicitudPagoDTO solicitudPago,
                long idUsuario
        ) {
            this.solicitudCita = solicitudCita;
            this.solicitudPago = solicitudPago;
            this.idUsuario = idUsuario;
            return resultado;
        }
    }
}
