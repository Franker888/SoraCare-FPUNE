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
import py.fpune.soracare.dto.CitaAgendaMedicoDTO;
import py.fpune.soracare.dto.RegistroExcepcionHorarioDTO;
import py.fpune.soracare.dto.ResumenDisponibilidadMedicoDTO;
import py.fpune.soracare.dto.UsuarioAutenticadoDTO;
import py.fpune.soracare.mvc.model.EstadoMedico;
import py.fpune.soracare.mvc.model.ExcepcionHorarioMedico;
import py.fpune.soracare.mvc.model.HorarioMedico;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.TipoExcepcionHorario;
import py.fpune.soracare.service.GestionDisponibilidadMedicoService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class DisponibilidadMedicoRestControllerTest {

    @Test
    void consultaSolamenteLaDisponibilidadDelMedicoAutenticado()
            throws Exception {
        GestionDisponibilidadMedicoServicePrueba servicio =
                new GestionDisponibilidadMedicoServicePrueba();
        servicio.resumen = resumen();

        MvcResult resultado = crearMockMvc(servicio).perform(
                        get("/medicos/me/disponibilidad")
                                .param("fecha", "2026-09-10")
                                .session(sesion(usuario("MEDICO")))
                )
                .andReturn();

        assertEquals(200, resultado.getResponse().getStatus());
        assertEquals(7L, servicio.ultimaPersonaConsultada);
        assertEquals(LocalDate.parse("2026-09-10"), servicio.ultimaFecha);

        JsonObject respuesta = JsonParser.parseString(
                resultado.getResponse().getContentAsString()
        ).getAsJsonObject();
        assertEquals(9L, respuesta.get("idMedico").getAsLong());
        assertEquals(1, respuesta.getAsJsonArray("horarios").size());
        assertEquals(1, respuesta.getAsJsonArray("excepciones").size());

        JsonArray citas = respuesta.getAsJsonArray("citasAfectadas");
        assertEquals(1, citas.size());
        JsonObject cita = citas.get(0).getAsJsonObject();
        assertEquals(41L, cita.get("idCita").getAsLong());
        assertFalse(cita.has("idPaciente"));
        assertFalse(cita.has("cedulaPaciente"));
        assertFalse(cita.has("numeroHistoriaClinica"));
        assertFalse(cita.has("motivoConsulta"));
        assertFalse(resultado.getResponse().getContentAsString()
                .contains("CEDULA-PRIVADA"));
    }

    @Test
    void rechazaLaConsultaSinRolMedico() throws Exception {
        MvcResult resultado = crearMockMvc(
                new GestionDisponibilidadMedicoServicePrueba()
        ).perform(get("/medicos/me/disponibilidad")
                        .param("fecha", "2026-09-10")
                        .session(sesion(usuario("PACIENTE"))))
                .andReturn();

        assertEquals(403, resultado.getResponse().getStatus());
        verificarCodigo(resultado, "ACCESO_DENEGADO");
    }

    @Test
    void creaUnBloqueoParaElMedicoDeLaSesion() throws Exception {
        GestionDisponibilidadMedicoServicePrueba servicio =
                new GestionDisponibilidadMedicoServicePrueba();
        servicio.bloqueoCreado = excepcion();

        MvcResult resultado = crearMockMvc(servicio).perform(
                        post("/medicos/me/disponibilidad/bloqueos")
                                .session(sesion(usuario("MEDICO")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "idMedico": 999,
                                          "fecha": "2026-09-10",
                                          "horaInicio": "08:00",
                                          "horaFin": "10:00",
                                          "tipo": "BLOQUEO_PARCIAL",
                                          "motivo": "Capacitación"
                                        }
                                        """))
                .andReturn();

        assertEquals(201, resultado.getResponse().getStatus());
        assertEquals(7L, servicio.ultimaPersonaBloqueo);
        assertEquals("", servicio.ultimoBloqueo.medicoEspecialidad());
        assertEquals("2026-09-10", servicio.ultimoBloqueo.fecha());
        assertEquals("BLOQUEO_PARCIAL", servicio.ultimoBloqueo.tipo());

        JsonObject respuesta = JsonParser.parseString(
                resultado.getResponse().getContentAsString()
        ).getAsJsonObject();
        assertEquals(31L, respuesta.get("idExcepcion").getAsLong());
        assertFalse(respuesta.has("idMedico"));
    }

    @Test
    void actualizaElEstadoDeUnBloqueoPropio() throws Exception {
        GestionDisponibilidadMedicoServicePrueba servicio =
                new GestionDisponibilidadMedicoServicePrueba();

        MvcResult resultado = crearMockMvc(servicio).perform(
                        patch("/medicos/me/disponibilidad/bloqueos/31")
                                .session(sesion(usuario("MEDICO")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"activo\":false}"))
                .andReturn();

        assertEquals(204, resultado.getResponse().getStatus());
        assertEquals(7L, servicio.ultimaPersonaEstado);
        assertEquals(31L, servicio.ultimaExcepcionEstado);
        assertFalse(servicio.ultimoEstado);
    }

    @Test
    void exigeUnEstadoExplicitoParaElBloqueo() throws Exception {
        GestionDisponibilidadMedicoServicePrueba servicio =
                new GestionDisponibilidadMedicoServicePrueba();

        MvcResult resultado = crearMockMvc(servicio).perform(
                        patch("/medicos/me/disponibilidad/bloqueos/31")
                                .session(sesion(usuario("MEDICO")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andReturn();

        assertEquals(400, resultado.getResponse().getStatus());
        verificarCodigo(resultado, "SOLICITUD_INVALIDA");
        assertEquals(0L, servicio.ultimaExcepcionEstado);
    }

    @Test
    void cancelaUnaCitaAsignadaConLaIdentidadDelMedicoAutenticado()
            throws Exception {
        GestionDisponibilidadMedicoServicePrueba servicio =
                new GestionDisponibilidadMedicoServicePrueba();

        MvcResult resultado = crearMockMvc(servicio).perform(
                        patch("/medicos/me/disponibilidad/citas/41/cancelacion")
                                .session(sesion(usuario("MEDICO")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"motivo\":\"Congreso médico\"}"))
                .andReturn();

        assertEquals(204, resultado.getResponse().getStatus());
        assertEquals(7L, servicio.ultimaPersonaCancelacion);
        assertEquals(5L, servicio.ultimoUsuarioCancelacion);
        assertEquals(41L, servicio.ultimaCitaCancelacion);
        assertEquals("Congreso médico", servicio.ultimoMotivoCancelacion);
    }

    private static MockMvc crearMockMvc(
            GestionDisponibilidadMedicoService servicio
    ) {
        return MockMvcBuilders.standaloneSetup(
                        new DisponibilidadMedicoRestController(servicio)
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
                "medico.prueba",
                false,
                Set.of(rol)
        );
    }

    private static ResumenDisponibilidadMedicoDTO resumen() {
        Medico medico = new Medico();
        medico.setIdMedico(9L);
        medico.setEstado(EstadoMedico.ACTIVO);
        medico.setCedula("CEDULA-PRIVADA");

        HorarioMedico horario = new HorarioMedico();
        horario.setIdHorario(12L);
        horario.setIdMedico(9L);
        horario.setIdEspecialidad(3);
        horario.setIdConsultorio(4L);
        horario.setDiaSemana(4);
        horario.setHoraInicio(LocalTime.parse("08:00"));
        horario.setHoraFin(LocalTime.parse("12:00"));
        horario.setDuracionTurnoMinutos(30);
        horario.setFechaDesde(LocalDate.parse("2026-08-01"));
        horario.setActivo(true);

        CitaAgendaMedicoDTO cita = new CitaAgendaMedicoDTO(
                41L,
                22L,
                OffsetDateTime.parse("2026-09-10T08:30:00-03:00"),
                OffsetDateTime.parse("2026-09-10T09:00:00-03:00"),
                "Paciente Prueba",
                "CEDULA-PRIVADA",
                "HC-PRIVADA",
                "Clínica Médica",
                "Consultorio 03",
                "Motivo privado",
                "Observación privada",
                "CONFIRMADA",
                "Confirmada"
        );

        return new ResumenDisponibilidadMedicoDTO(
                medico,
                LocalDate.parse("2026-09-10"),
                List.of(horario),
                List.of(excepcion()),
                List.of(cita)
        );
    }

    private static ExcepcionHorarioMedico excepcion() {
        ExcepcionHorarioMedico excepcion = new ExcepcionHorarioMedico();
        excepcion.setIdExcepcion(31L);
        excepcion.setIdMedico(9L);
        excepcion.setFecha(LocalDate.parse("2026-09-10"));
        excepcion.setHoraInicio(LocalTime.parse("08:00"));
        excepcion.setHoraFin(LocalTime.parse("10:00"));
        excepcion.setTipo(TipoExcepcionHorario.BLOQUEO_PARCIAL);
        excepcion.setMotivo("Capacitación");
        excepcion.setActivo(true);
        return excepcion;
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

    private static final class GestionDisponibilidadMedicoServicePrueba
            extends GestionDisponibilidadMedicoService {

        private ResumenDisponibilidadMedicoDTO resumen;
        private ExcepcionHorarioMedico bloqueoCreado;
        private long ultimaPersonaConsultada;
        private LocalDate ultimaFecha;
        private long ultimaPersonaBloqueo;
        private RegistroExcepcionHorarioDTO ultimoBloqueo;
        private long ultimaPersonaEstado;
        private long ultimaExcepcionEstado;
        private boolean ultimoEstado;
        private long ultimaPersonaCancelacion;
        private long ultimoUsuarioCancelacion;
        private long ultimaCitaCancelacion;
        private String ultimoMotivoCancelacion;

        @Override
        public ResumenDisponibilidadMedicoDTO consultar(
                long idPersona,
                LocalDate fecha
        ) {
            ultimaPersonaConsultada = idPersona;
            ultimaFecha = fecha;
            return resumen;
        }

        @Override
        public ExcepcionHorarioMedico registrarBloqueo(
                long idPersona,
                RegistroExcepcionHorarioDTO datos
        ) {
            ultimaPersonaBloqueo = idPersona;
            ultimoBloqueo = datos;
            return bloqueoCreado;
        }

        @Override
        public void actualizarEstado(
                long idPersona,
                long idExcepcion,
                boolean activo
        ) {
            ultimaPersonaEstado = idPersona;
            ultimaExcepcionEstado = idExcepcion;
            ultimoEstado = activo;
        }

        @Override
        public void cancelarCita(
                long idPersona,
                long idUsuario,
                long idCita,
                String motivo
        ) {
            ultimaPersonaCancelacion = idPersona;
            ultimoUsuarioCancelacion = idUsuario;
            ultimaCitaCancelacion = idCita;
            ultimoMotivoCancelacion = motivo;
        }
    }
}
