package py.fpune.soracare.service;

import py.fpune.soracare.dto.AgendaMedicoDTO;
import py.fpune.soracare.dto.CitaAgendaMedicoDTO;
import py.fpune.soracare.dto.RegistroExcepcionHorarioDTO;
import py.fpune.soracare.dto.ResumenDisponibilidadMedicoDTO;
import py.fpune.soracare.mvc.model.EstadoMedico;
import py.fpune.soracare.mvc.model.ExcepcionHorarioMedico;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.TipoExcepcionHorario;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

public class GestionDisponibilidadMedicoService {

    private static final Set<String> ESTADOS_FINALES =
            Set.of("CANCELADA", "ATENDIDA", "AUSENTE");

    private final MedicoService medicoService = new MedicoService();
    private final AgendaMedicoService agendaService = new AgendaMedicoService();
    private final ExcepcionHorarioMedicoService excepcionService =
            new ExcepcionHorarioMedicoService();
    private final CitaService citaService = new CitaService();

    public ResumenDisponibilidadMedicoDTO consultar(
            long idPersona,
            LocalDate fecha
    ) throws SQLException {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de disponibilidad es obligatoria.");
        }
        Medico medico = obtenerMedicoActivo(idPersona);
        return new ResumenDisponibilidadMedicoDTO(
                medico,
                fecha,
                excepcionService.listarPorMedico(medico.getIdMedico()),
                listarCitasAfectadas(idPersona, fecha, null, null)
        );
    }

    public ExcepcionHorarioMedico registrarBloqueo(
            long idPersona,
            RegistroExcepcionHorarioDTO datos
    ) throws SQLException {
        BloqueoValidado bloqueo = validarBloqueo(datos);
        Medico medico = obtenerMedicoActivo(idPersona);
        List<CitaAgendaMedicoDTO> afectadas = listarCitasAfectadas(
                idPersona,
                bloqueo.fecha(),
                bloqueo.horaInicio(),
                bloqueo.horaFin()
        );
        if (!afectadas.isEmpty()) {
            throw new IllegalArgumentException(mensajeCitasAfectadas(afectadas.size()));
        }

        return excepcionService.registrar(new RegistroExcepcionHorarioDTO(
                String.valueOf(medico.getIdMedico()),
                "",
                bloqueo.fecha().toString(),
                bloqueo.horaInicio() == null ? "" : bloqueo.horaInicio().toString(),
                bloqueo.horaFin() == null ? "" : bloqueo.horaFin().toString(),
                bloqueo.tipo().name(),
                datos.motivo()
        ));
    }

    public void actualizarEstado(
            long idPersona,
            long idExcepcion,
            boolean activo
    ) throws SQLException {
        Medico medico = obtenerMedicoActivo(idPersona);
        ExcepcionHorarioMedico excepcion = excepcionService
                .buscarPorId(idExcepcion)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La excepción seleccionada no existe."
                ));
        validarPropiedad(medico, excepcion);

        if (activo) {
            if (excepcion.getFecha().isBefore(
                    LocalDate.now(DisponibilidadService.ZONA_HORARIA))) {
                throw new IllegalArgumentException(
                        "No podés reactivar un bloqueo de una fecha pasada."
                );
            }
            List<CitaAgendaMedicoDTO> afectadas = listarCitasAfectadas(
                    idPersona,
                    excepcion.getFecha(),
                    excepcion.getHoraInicio(),
                    excepcion.getHoraFin()
            );
            if (!afectadas.isEmpty()) {
                throw new IllegalArgumentException(
                        mensajeCitasAfectadas(afectadas.size())
                );
            }
        }
        excepcionService.actualizarEstado(idExcepcion, activo);
    }

    public void cancelarCita(
            long idPersona,
            long idUsuario,
            long idCita,
            String motivo
    ) throws SQLException {
        Medico medico = obtenerMedicoActivo(idPersona);
        citaService.cancelarPorMedico(
                idCita,
                medico.getIdMedico(),
                idUsuario,
                motivo
        );
    }

    static BloqueoValidado validarBloqueo(RegistroExcepcionHorarioDTO datos) {
        if (datos == null) {
            throw new IllegalArgumentException("Los datos del bloqueo son obligatorios.");
        }
        LocalDate fecha = convertirFecha(datos.fecha());
        if (fecha.isBefore(LocalDate.now(DisponibilidadService.ZONA_HORARIA))) {
            throw new IllegalArgumentException("No podés bloquear una fecha pasada.");
        }
        TipoExcepcionHorario tipo = convertirTipo(datos.tipo());
        if (tipo == TipoExcepcionHorario.DISPONIBILIDAD_EXTRA) {
            throw new IllegalArgumentException(
                    "Desde el portal médico sólo se permiten bloqueos de agenda."
            );
        }
        LocalTime inicio = convertirHora(datos.horaInicio());
        LocalTime fin = convertirHora(datos.horaFin());
        if (tipo == TipoExcepcionHorario.BLOQUEO_TOTAL) {
            if (inicio != null || fin != null) {
                throw new IllegalArgumentException(
                        "El bloqueo total no debe indicar horas."
                );
            }
        } else if (inicio == null || fin == null || !inicio.isBefore(fin)) {
            throw new IllegalArgumentException(
                    "Indicá un intervalo de horas válido."
            );
        }
        return new BloqueoValidado(fecha, inicio, fin, tipo);
    }

    private List<CitaAgendaMedicoDTO> listarCitasAfectadas(
            long idPersona,
            LocalDate fecha,
            LocalTime inicio,
            LocalTime fin
    ) throws SQLException {
        AgendaMedicoDTO agenda = agendaService.consultar(idPersona, fecha);
        OffsetDateTime ahora = OffsetDateTime.now(DisponibilidadService.ZONA_HORARIA);
        return agenda.citas().stream()
                .filter(cita -> !ESTADOS_FINALES.contains(cita.codigoEstado()))
                .filter(cita -> cita.fechaHoraFin().isAfter(ahora))
                .filter(cita -> inicio == null || seSuperpone(cita, inicio, fin))
                .toList();
    }

    private Medico obtenerMedicoActivo(long idPersona) throws SQLException {
        if (idPersona <= 0) {
            throw new IllegalArgumentException("La cuenta médica no es válida.");
        }
        Medico medico = medicoService.buscarPorIdPersona(idPersona)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta no está vinculada con un médico registrado."
                ));
        if (medico.getEstado() != EstadoMedico.ACTIVO) {
            throw new IllegalArgumentException(
                    "El médico no se encuentra activo para gestionar su disponibilidad."
            );
        }
        return medico;
    }

    private static boolean seSuperpone(
            CitaAgendaMedicoDTO cita,
            LocalTime inicio,
            LocalTime fin
    ) {
        LocalTime inicioCita = cita.fechaHoraInicio()
                .atZoneSameInstant(DisponibilidadService.ZONA_HORARIA)
                .toLocalTime();
        LocalTime finCita = cita.fechaHoraFin()
                .atZoneSameInstant(DisponibilidadService.ZONA_HORARIA)
                .toLocalTime();
        return inicio.isBefore(finCita) && fin.isAfter(inicioCita);
    }

    private static void validarPropiedad(
            Medico medico,
            ExcepcionHorarioMedico excepcion
    ) {
        if (!medico.getIdMedico().equals(excepcion.getIdMedico())) {
            throw new IllegalArgumentException(
                    "No podés modificar la disponibilidad de otro médico."
            );
        }
    }

    private static String mensajeCitasAfectadas(int cantidad) {
        return "El bloqueo afecta " + cantidad + " cita"
                + (cantidad == 1 ? "" : "s")
                + ". Cancelá o reprogramá esas citas antes de guardar el bloqueo.";
    }

    private static LocalDate convertirFecha(String valor) {
        try {
            return LocalDate.parse(valor == null ? "" : valor.strip());
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException(
                    "La fecha del bloqueo no es válida.",
                    error
            );
        }
    }

    private static LocalTime convertirHora(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return LocalTime.parse(valor.strip());
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException(
                    "La hora indicada no es válida.",
                    error
            );
        }
    }

    private static TipoExcepcionHorario convertirTipo(String valor) {
        try {
            return TipoExcepcionHorario.valueOf(valor == null ? "" : valor.strip());
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException(
                    "El tipo de bloqueo no es válido.",
                    error
            );
        }
    }

    record BloqueoValidado(
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            TipoExcepcionHorario tipo
    ) {
    }
}
