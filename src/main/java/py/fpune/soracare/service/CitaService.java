package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.CitaDAO;
import py.fpune.soracare.dao.EstadoCitaDAO;
import py.fpune.soracare.dao.PacienteDAO;
import py.fpune.soracare.dto.SlotDisponibleDTO;
import py.fpune.soracare.dto.SolicitudCitaDTO;
import py.fpune.soracare.mvc.model.Cita;
import py.fpune.soracare.mvc.model.EstadoCita;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public class CitaService {

    private static final String ESTADO_SOLICITADA = "SOLICITADA";
    private static final String ESTADO_CANCELADA = "CANCELADA";
    private static final List<String> CANALES =
            List.of("WEB", "RECEPCION", "TELEFONO", "ADMINISTRACION");

    private final CitaDAO citaDAO = new CitaDAO();
    private final EstadoCitaDAO estadoDAO = new EstadoCitaDAO();
    private final PacienteDAO pacienteDAO = new PacienteDAO();
    private final DisponibilidadService disponibilidadService = new DisponibilidadService();

    public Cita solicitar(SolicitudCitaDTO datos, Long idUsuarioCreador)
            throws SQLException {
        if (datos == null) throw new IllegalArgumentException("Los datos de la cita son obligatorios.");
        long idPaciente = convertirLong(datos.idPaciente(), "Seleccioná un paciente.");
        Long idHorario = convertirLongOpcional(datos.idHorario());
        Long idExcepcion = convertirLongOpcional(datos.idExcepcion());
        long idMedico = convertirLong(datos.idMedico(), "El médico del turno no es válido.");
        int idEspecialidad = convertirEntero(datos.idEspecialidad(), "La especialidad del turno no es válida.");
        long idConsultorio = convertirLong(datos.idConsultorio(), "El consultorio del turno no es válido.");
        LocalDate fecha = convertirFecha(datos.fecha());
        LocalTime horaInicio = convertirHora(datos.horaInicio());
        String motivo = normalizarObligatorio(datos.motivoConsulta(), 500,
                "El motivo de consulta es obligatorio.",
                "El motivo de consulta no puede superar 500 caracteres.");
        String observaciones = normalizarOpcional(datos.observaciones(), 1000,
                "Las observaciones no pueden superar 1000 caracteres.");
        String canal = normalizarCanal(datos.canalReserva());

        SlotDisponibleDTO slot = disponibilidadService
                .listar(fecha, idMedico, idEspecialidad).stream()
                .filter(actual -> actual.idMedico() == idMedico
                        && actual.idEspecialidad() == idEspecialidad
                        && actual.idConsultorio() == idConsultorio
                        && actual.horaInicio().equals(horaInicio)
                        && iguales(actual.idHorario(), idHorario)
                        && iguales(actual.idExcepcion(), idExcepcion))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "El turno seleccionado ya no está disponible."));

        OffsetDateTime inicio = fecha.atTime(slot.horaInicio())
                .atZone(DisponibilidadService.ZONA_HORARIA).toOffsetDateTime();
        OffsetDateTime fin = fecha.atTime(slot.horaFin())
                .atZone(DisponibilidadService.ZONA_HORARIA).toOffsetDateTime();

        try (Connection conexion = ConexionBD.obtenerConexion()) {
            try {
                conexion.setAutoCommit(false);
                if (pacienteDAO.buscarPorId(conexion, idPaciente).isEmpty()) {
                    throw new IllegalArgumentException("El paciente seleccionado no existe.");
                }
                EstadoCita estado = estadoDAO.buscarPorCodigo(conexion, ESTADO_SOLICITADA)
                        .orElseThrow(() -> new SQLException("No existe el estado inicial de citas."));
                if (citaDAO.existeSuperposicion(conexion, idPaciente, idMedico,
                        idConsultorio, inicio, fin)) {
                    throw new IllegalArgumentException(
                            "El paciente, médico o consultorio ya posee una cita en ese horario.");
                }

                Cita cita = new Cita();
                cita.setIdPaciente(idPaciente);
                cita.setIdMedico(idMedico);
                cita.setIdEspecialidad(idEspecialidad);
                cita.setIdConsultorio(idConsultorio);
                cita.setIdHorario(idHorario);
                cita.setIdEstadoCita(estado.getIdEstadoCita());
                cita.setFechaHoraInicio(inicio);
                cita.setFechaHoraFin(fin);
                cita.setMotivoConsulta(motivo);
                cita.setObservaciones(observaciones);
                cita.setCanalReserva(canal);
                cita.setIdUsuarioCreador(idUsuarioCreador);

                Cita creada = citaDAO.crear(conexion, cita);
                conexion.commit();
                return creada;
            } catch (Exception error) {
                rollback(conexion, error);
                if (error instanceof IllegalArgumentException validacion) throw validacion;
                if (error instanceof SQLException sql) {
                    if (esSuperposicion(sql)) {
                        throw new IllegalArgumentException(
                                "El turno acaba de ser ocupado. Elegí otro horario.", sql);
                    }
                    throw sql;
                }
                throw new SQLException("No fue posible crear la cita.", error);
            }
        }
    }

    public List<Cita> listarPorFecha(LocalDate fecha) throws SQLException {
        OffsetDateTime desde = fecha.atStartOfDay(DisponibilidadService.ZONA_HORARIA).toOffsetDateTime();
        OffsetDateTime hasta = fecha.plusDays(1).atStartOfDay(DisponibilidadService.ZONA_HORARIA).toOffsetDateTime();
        return citaDAO.listarEntre(desde, hasta);
    }

    public List<Cita> listarProximas() throws SQLException {
        return citaDAO.listarProximas(200);
    }

    public List<Cita> listarPorPaciente(long idPaciente) throws SQLException {
        if (idPaciente <= 0) throw new IllegalArgumentException("El paciente no es valido.");
        return citaDAO.listarPorPaciente(idPaciente, 100);
    }

    public void cancelar(long idCita, long idUsuario, String motivo) throws SQLException {
        if (idCita <= 0 || idUsuario <= 0) throw new IllegalArgumentException("La cita seleccionada no es válida.");
        String motivoNormalizado = normalizarObligatorio(motivo, 500,
                "El motivo de cancelación es obligatorio.",
                "El motivo de cancelación no puede superar 500 caracteres.");
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            try {
                conexion.setAutoCommit(false);
                Cita cita = citaDAO.buscarPorId(conexion, idCita)
                        .orElseThrow(() -> new IllegalArgumentException("La cita seleccionada no existe."));
                if (ESTADO_CANCELADA.equals(cita.getCodigoEstado())) {
                    throw new IllegalArgumentException("La cita ya está cancelada.");
                }
                if ("ATENDIDA".equals(cita.getCodigoEstado())) {
                    throw new IllegalArgumentException("Una cita atendida no puede cancelarse.");
                }
                EstadoCita cancelada = estadoDAO.buscarPorCodigo(conexion, ESTADO_CANCELADA)
                        .orElseThrow(() -> new SQLException("No existe el estado de cancelación."));
                citaDAO.cancelar(conexion, idCita, cancelada.getIdEstadoCita(), idUsuario, motivoNormalizado);
                conexion.commit();
            } catch (Exception error) {
                rollback(conexion, error);
                if (error instanceof IllegalArgumentException validacion) throw validacion;
                if (error instanceof SQLException sql) throw sql;
                throw new SQLException("No fue posible cancelar la cita.", error);
            }
        }
    }

    public void cancelarPorPaciente(long idCita, long idPaciente, long idUsuario, String motivo)
            throws SQLException {
        if (idCita <= 0 || idPaciente <= 0 || idUsuario <= 0) {
            throw new IllegalArgumentException("La cita seleccionada no es valida.");
        }
        String motivoNormalizado = normalizarObligatorio(motivo, 500,
                "El motivo de cancelacion es obligatorio.",
                "El motivo de cancelacion no puede superar 500 caracteres.");
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            try {
                conexion.setAutoCommit(false);
                Cita cita = citaDAO.buscarPorId(conexion, idCita)
                        .orElseThrow(() -> new IllegalArgumentException("La cita seleccionada no existe."));
                if (!Long.valueOf(idPaciente).equals(cita.getIdPaciente())) {
                    throw new IllegalArgumentException("No podes cancelar una cita de otro paciente.");
                }
                if (ESTADO_CANCELADA.equals(cita.getCodigoEstado())) {
                    throw new IllegalArgumentException("La cita ya esta cancelada.");
                }
                if ("ATENDIDA".equals(cita.getCodigoEstado())) {
                    throw new IllegalArgumentException("Una cita atendida no puede cancelarse.");
                }
                EstadoCita cancelada = estadoDAO.buscarPorCodigo(conexion, ESTADO_CANCELADA)
                        .orElseThrow(() -> new SQLException("No existe el estado de cancelacion."));
                citaDAO.cancelar(conexion, idCita, cancelada.getIdEstadoCita(), idUsuario, motivoNormalizado);
                conexion.commit();
            } catch (Exception error) {
                rollback(conexion, error);
                if (error instanceof IllegalArgumentException validacion) throw validacion;
                if (error instanceof SQLException sql) throw sql;
                throw new SQLException("No fue posible cancelar la cita.", error);
            }
        }
    }

    private static boolean iguales(Long a, Long b) {
        return a == null ? b == null : a.equals(b);
    }

    private static long convertirLong(String valor, String mensaje) {
        try {
            long numero = Long.parseLong(valor == null ? "" : valor.strip());
            if (numero <= 0) throw new NumberFormatException();
            return numero;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(mensaje, error);
        }
    }

    private static Long convertirLongOpcional(String valor) {
        return valor == null || valor.isBlank() ? null
                : convertirLong(valor, "El origen del turno no es válido.");
    }

    private static int convertirEntero(String valor, String mensaje) {
        try {
            int numero = Integer.parseInt(valor == null ? "" : valor.strip());
            if (numero <= 0) throw new NumberFormatException();
            return numero;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(mensaje, error);
        }
    }

    private static LocalDate convertirFecha(String valor) {
        try {
            return LocalDate.parse(valor == null ? "" : valor.strip());
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("La fecha del turno no es válida.", error);
        }
    }

    private static LocalTime convertirHora(String valor) {
        try {
            return LocalTime.parse(valor == null ? "" : valor.strip());
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("La hora del turno no es válida.", error);
        }
    }

    private static String normalizarCanal(String valor) {
        String canal = valor == null || valor.isBlank()
                ? "WEB" : valor.strip().toUpperCase(Locale.ROOT);
        if (!CANALES.contains(canal)) throw new IllegalArgumentException("El canal de reserva no es válido.");
        return canal;
    }

    private static String normalizarObligatorio(
            String valor, int maximo, String mensajeVacio, String mensajeLargo
    ) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(mensajeVacio);
        String texto = valor.strip();
        if (texto.length() > maximo) throw new IllegalArgumentException(mensajeLargo);
        return texto;
    }

    private static String normalizarOpcional(String valor, int maximo, String mensajeLargo) {
        if (valor == null || valor.isBlank()) return null;
        String texto = valor.strip();
        if (texto.length() > maximo) throw new IllegalArgumentException(mensajeLargo);
        return texto;
    }

    private static boolean esSuperposicion(SQLException error) {
        SQLException actual = error;
        while (actual != null) {
            if (actual.getMessage() != null
                    && actual.getMessage().toLowerCase(Locale.ROOT).contains("ya posee una cita")) return true;
            actual = actual.getNextException();
        }
        return false;
    }

    private static void rollback(Connection conexion, Exception original) {
        try {
            conexion.rollback();
        } catch (SQLException rollback) {
            original.addSuppressed(rollback);
        }
    }
}
