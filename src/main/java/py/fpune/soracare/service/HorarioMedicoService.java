package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.ConsultorioDAO;
import py.fpune.soracare.dao.EspecialidadDAO;
import py.fpune.soracare.dao.HorarioMedicoDAO;
import py.fpune.soracare.dao.MedicoDAO;
import py.fpune.soracare.dao.MedicoEspecialidadDAO;
import py.fpune.soracare.dao.SedeDAO;
import py.fpune.soracare.dto.RegistroHorarioMedicoDTO;
import py.fpune.soracare.mvc.model.Consultorio;
import py.fpune.soracare.mvc.model.Especialidad;
import py.fpune.soracare.mvc.model.EstadoConsultorio;
import py.fpune.soracare.mvc.model.EstadoMedico;
import py.fpune.soracare.mvc.model.HorarioMedico;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.MedicoEspecialidad;
import py.fpune.soracare.mvc.model.Sede;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public class HorarioMedicoService {

    private static final String SQLSTATE_VIOLACION_UNICIDAD =
            "23505";

    private final HorarioMedicoDAO horarioMedicoDAO =
            new HorarioMedicoDAO();

    private final MedicoDAO medicoDAO =
            new MedicoDAO();

    private final EspecialidadDAO especialidadDAO =
            new EspecialidadDAO();

    private final MedicoEspecialidadDAO
            medicoEspecialidadDAO =
            new MedicoEspecialidadDAO();

    private final ConsultorioDAO consultorioDAO =
            new ConsultorioDAO();

    private final SedeDAO sedeDAO =
            new SedeDAO();

    public HorarioMedico registrar(
            RegistroHorarioMedicoDTO datos
    ) throws SQLException {

        HorarioMedico horario =
                convertirYValidar(
                        datos
                );

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            verificarReferenciasDisponibles(
                    conexion,
                    horario
            );
        }

        verificarSinSuperposicion(
                horario,
                0
        );

        try {
            return horarioMedicoDAO.crear(
                    horario
            );
        } catch (SQLException error) {
            if (esViolacionDeUnicidad(error)) {
                throw new IllegalArgumentException(
                        "Ya existe un horario igual para el médico, "
                                + "especialidad y consultorio.",
                        error
                );
            }
            throw error;
        }
    }

    public Optional<HorarioMedico> buscarPorId(
            long idHorario
    ) throws SQLException {

        validarIdHorario(
                idHorario
        );

        return horarioMedicoDAO.buscarPorId(
                idHorario
        );
    }

    public List<HorarioMedico> listarTodos()
            throws SQLException {

        return horarioMedicoDAO.listarTodos();
    }

    public List<HorarioMedico> listarPorMedico(long idMedico)
            throws SQLException {
        if (idMedico <= 0) {
            throw new IllegalArgumentException(
                    "El médico seleccionado no es válido."
            );
        }
        return horarioMedicoDAO.listarPorMedico(idMedico);
    }

    public List<HorarioMedico> listarVigentesEnFecha(
            LocalDate fecha
    ) throws SQLException {

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "La fecha de consulta es obligatoria."
            );
        }

        return horarioMedicoDAO.listarVigentesEnFecha(
                fecha
        );
    }

    public void actualizarEstado(
            long idHorario,
            boolean activo
    ) throws SQLException {

        validarIdHorario(
                idHorario
        );

        HorarioMedico horario =
                horarioMedicoDAO.buscarPorId(
                        idHorario
                ).orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "El horario seleccionado no existe."
                                )
                );

        if (horario.isActivo() == activo) {
            return;
        }

        if (activo) {
            try (
                    Connection conexion =
                            ConexionBD.obtenerConexion()
            ) {
                verificarReferenciasDisponibles(
                        conexion,
                        horario
                );
            }

            verificarSinSuperposicion(
                    horario,
                    idHorario
            );
        }

        horarioMedicoDAO.actualizarActivo(
                idHorario,
                activo
        );
    }

    private HorarioMedico convertirYValidar(
            RegistroHorarioMedicoDTO datos
    ) {

        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos del horario no pueden ser nulos."
            );
        }

        HorarioMedico horario =
                new HorarioMedico();

        horario.setIdMedico(
                convertirLongObligatorio(
                        datos.getIdMedico(),
                        "El médico es obligatorio."
                )
        );

        horario.setIdEspecialidad(
                convertirEnteroObligatorio(
                        datos.getIdEspecialidad(),
                        "La especialidad es obligatoria."
                )
        );

        horario.setIdConsultorio(
                convertirLongObligatorio(
                        datos.getIdConsultorio(),
                        "El consultorio es obligatorio."
                )
        );

        horario.setDiaSemana(
                convertirEnteroObligatorio(
                        datos.getDiaSemana(),
                        "El día de la semana es obligatorio."
                )
        );

        LocalTime horaInicio =
                convertirHora(
                        datos.getHoraInicio(),
                        "La hora de inicio es obligatoria.",
                        "La hora de inicio no es válida."
                );

        LocalTime horaFin =
                convertirHora(
                        datos.getHoraFin(),
                        "La hora de fin es obligatoria.",
                        "La hora de fin no es válida."
                );

        if (!horaInicio.isBefore(
                horaFin
        )) {
            throw new IllegalArgumentException(
                    "La hora de inicio debe ser anterior a la hora de fin."
            );
        }

        horario.setHoraInicio(
                horaInicio
        );

        horario.setHoraFin(
                horaFin
        );

        int duracionTurnoMinutos =
                convertirEnteroObligatorio(
                        datos.getDuracionTurnoMinutos(),
                        "La duración del turno es obligatoria."
                );

        horario.setDuracionTurnoMinutos(
                duracionTurnoMinutos
        );

        long minutosDisponibles =
                Duration.between(
                        horaInicio,
                        horaFin
                ).toMinutes();

        if (minutosDisponibles
                < duracionTurnoMinutos) {

            throw new IllegalArgumentException(
                    "La duración del turno supera el rango horario disponible."
            );
        }

        LocalDate fechaDesde =
                convertirFechaDesde(
                        datos.getFechaDesde()
                );

        LocalDate fechaHasta =
                convertirFechaOpcional(
                        datos.getFechaHasta(),
                        "La fecha final no es válida."
                );

        if (fechaHasta != null
                && fechaHasta.isBefore(
                fechaDesde
        )) {
            throw new IllegalArgumentException(
                    "La fecha final no puede ser anterior a la fecha inicial."
            );
        }

        horario.setFechaDesde(
                fechaDesde
        );

        horario.setFechaHasta(
                fechaHasta
        );

        horario.setActivo(
                true
        );

        return horario;
    }

    private void verificarReferenciasDisponibles(
            Connection conexion,
            HorarioMedico horario
    ) throws SQLException {

        Medico medico =
                medicoDAO.buscarPorId(
                        conexion,
                        horario.getIdMedico()
                ).orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "El médico seleccionado no existe."
                                )
                );

        if (medico.getEstado()
                != EstadoMedico.ACTIVO) {

            throw new IllegalArgumentException(
                    "El médico seleccionado no se encuentra activo."
            );
        }

        Especialidad especialidad =
                especialidadDAO.buscarPorId(
                        conexion,
                        horario.getIdEspecialidad()
                ).orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "La especialidad seleccionada no existe."
                                )
                );

        if (!especialidad.isActivo()) {
            throw new IllegalArgumentException(
                    "La especialidad seleccionada se encuentra inactiva."
            );
        }

        MedicoEspecialidad relacion =
                medicoEspecialidadDAO.buscarPorIds(
                        conexion,
                        horario.getIdMedico(),
                        horario.getIdEspecialidad()
                ).orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "El médico no tiene asignada "
                                                + "la especialidad seleccionada."
                                )
                );

        if (!relacion.isActivo()) {
            throw new IllegalArgumentException(
                    "La especialidad del médico se encuentra inactiva."
            );
        }

        Consultorio consultorio =
                consultorioDAO.buscarPorId(
                        conexion,
                        horario.getIdConsultorio()
                ).orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "El consultorio seleccionado no existe."
                                )
                );

        if (consultorio.getEstado()
                != EstadoConsultorio.DISPONIBLE) {

            throw new IllegalArgumentException(
                    "El consultorio seleccionado no está disponible."
            );
        }

        Sede sede =
                sedeDAO.buscarPorId(
                        conexion,
                        consultorio.getIdSede()
                ).orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "La sede del consultorio no existe."
                                )
                );

        if (!sede.isActivo()) {
            throw new IllegalArgumentException(
                    "La sede del consultorio se encuentra inactiva."
            );
        }
    }

    private void verificarSinSuperposicion(
            HorarioMedico horario,
            long idHorarioExcluido
    ) throws SQLException {

        if (horarioMedicoDAO.existeSuperposicion(
                horario,
                idHorarioExcluido
        )) {
            throw new IllegalArgumentException(
                    "El médico o el consultorio ya posee "
                            + "un horario superpuesto en ese día y vigencia."
            );
        }
    }

    private static LocalTime convertirHora(
            String valor,
            String mensajeObligatorio,
            String mensajeInvalido
    ) {

        if (valor == null
                || valor.isBlank()) {

            throw new IllegalArgumentException(
                    mensajeObligatorio
            );
        }

        try {
            return LocalTime.parse(
                    valor.strip()
            );

        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException(
                    mensajeInvalido,
                    error
            );
        }
    }

    private static LocalDate convertirFechaDesde(
            String valor
    ) {

        if (valor == null
                || valor.isBlank()) {

            return LocalDate.now();
        }

        return convertirFechaOpcional(
                valor,
                "La fecha inicial no es válida."
        );
    }

    private static LocalDate convertirFechaOpcional(
            String valor,
            String mensajeInvalido
    ) {

        if (valor == null
                || valor.isBlank()) {

            return null;
        }

        try {
            return LocalDate.parse(
                    valor.strip()
            );

        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException(
                    mensajeInvalido,
                    error
            );
        }
    }

    private static long convertirLongObligatorio(
            String valor,
            String mensajeObligatorio
    ) {

        if (valor == null
                || valor.isBlank()) {

            throw new IllegalArgumentException(
                    mensajeObligatorio
            );
        }

        try {
            long numero =
                    Long.parseLong(
                            valor.strip()
                    );

            if (numero <= 0) {
                throw new NumberFormatException();
            }

            return numero;

        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    mensajeObligatorio,
                    error
            );
        }
    }

    private static int convertirEnteroObligatorio(
            String valor,
            String mensajeObligatorio
    ) {

        if (valor == null
                || valor.isBlank()) {

            throw new IllegalArgumentException(
                    mensajeObligatorio
            );
        }

        try {
            int numero =
                    Integer.parseInt(
                            valor.strip()
                    );

            if (numero <= 0) {
                throw new NumberFormatException();
            }

            return numero;

        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    mensajeObligatorio,
                    error
            );
        }
    }

    private static void validarIdHorario(
            long idHorario
    ) {

        if (idHorario <= 0) {
            throw new IllegalArgumentException(
                    "El ID del horario debe ser mayor que cero."
            );
        }
    }

    private static boolean esViolacionDeUnicidad(
            Throwable error
    ) {
        Throwable causaActual = error;

        while (causaActual != null) {
            if (causaActual instanceof SQLException errorSql) {
                SQLException errorActual = errorSql;

                while (errorActual != null) {
                    if (SQLSTATE_VIOLACION_UNICIDAD.equals(
                            errorActual.getSQLState()
                    )) {
                        return true;
                    }

                    errorActual = errorActual.getNextException();
                }
            }

            causaActual = causaActual.getCause();
        }

        return false;
    }
}
