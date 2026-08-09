package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.EspecialidadDAO;
import py.fpune.soracare.dao.MedicoDAO;
import py.fpune.soracare.dao.MedicoEspecialidadDAO;
import py.fpune.soracare.dto.AsignacionMedicoEspecialidadDTO;
import py.fpune.soracare.mvc.model.Especialidad;
import py.fpune.soracare.mvc.model.MedicoEspecialidad;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MedicoEspecialidadService {

    private final MedicoDAO medicoDAO =
            new MedicoDAO();

    private final EspecialidadDAO especialidadDAO =
            new EspecialidadDAO();

    private final MedicoEspecialidadDAO relacionDAO =
            new MedicoEspecialidadDAO();

    public MedicoEspecialidad asignar(
            AsignacionMedicoEspecialidadDTO datos
    ) throws SQLException {

        if (datos == null) {
            throw new SQLException(
                    "Los datos de la asignación "
                            + "no pueden ser nulos."
            );
        }

        long idMedico =
                convertirIdMedico(
                        datos.getIdMedico()
                );

        int idEspecialidad =
                convertirIdEspecialidad(
                        datos.getIdEspecialidad()
                );

        boolean principal =
                convertirPrincipal(
                        datos.getPrincipal()
                );

        LocalDate fechaDesde =
                convertirFechaDesde(
                        datos.getFechaDesde()
                );

        MedicoEspecialidad relacion =
                new MedicoEspecialidad(
                        idMedico,
                        idEspecialidad,
                        principal,
                        fechaDesde
                );

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            conexion.setAutoCommit(
                    false
            );

            try {
                verificarMedicoExistente(
                        conexion,
                        idMedico
                );

                verificarEspecialidadDisponible(
                        conexion,
                        idEspecialidad
                );

                verificarRelacionNoExistente(
                        conexion,
                        idMedico,
                        idEspecialidad
                );

                if (principal) {
                    verificarPrincipalDisponible(
                            conexion,
                            idMedico
                    );
                }

                MedicoEspecialidad relacionCreada =
                        relacionDAO.crear(
                                conexion,
                                relacion
                        );

                conexion.commit();

                return relacionCreada;

            } catch (SQLException error) {

                ejecutarRollback(
                        conexion,
                        error
                );

                throw error;

            } catch (RuntimeException error) {

                ejecutarRollback(
                        conexion,
                        error
                );

                throw error;
            }
        }
    }

    public Optional<MedicoEspecialidad> buscarPorIds(
            long idMedico,
            int idEspecialidad
    ) throws SQLException {

        validarIdMedico(
                idMedico
        );

        validarIdEspecialidad(
                idEspecialidad
        );

        return relacionDAO.buscarPorIds(
                idMedico,
                idEspecialidad
        );
    }

    public Optional<MedicoEspecialidad> buscarPrincipalActiva(
            long idMedico
    ) throws SQLException {

        validarIdMedico(
                idMedico
        );

        return relacionDAO.buscarPrincipalActiva(
                idMedico
        );
    }

    public List<MedicoEspecialidad> listarPorMedico(
            long idMedico
    ) throws SQLException {

        validarIdMedico(
                idMedico
        );

        return relacionDAO.listarPorMedico(
                idMedico
        );
    }

    public List<MedicoEspecialidad> listarActivasPorMedico(
            long idMedico
    ) throws SQLException {

        validarIdMedico(
                idMedico
        );

        return relacionDAO.listarActivasPorMedico(
                idMedico
        );
    }

    private void verificarMedicoExistente(
            Connection conexion,
            long idMedico
    ) throws SQLException {

        if (medicoDAO.buscarPorId(
                conexion,
                idMedico
        ).isEmpty()) {

            throw new SQLException(
                    "El médico seleccionado no existe."
            );
        }
    }

    private void verificarEspecialidadDisponible(
            Connection conexion,
            int idEspecialidad
    ) throws SQLException {

        Optional<Especialidad> especialidad =
                especialidadDAO.buscarPorId(
                        conexion,
                        idEspecialidad
                );

        if (especialidad.isEmpty()) {
            throw new SQLException(
                    "La especialidad seleccionada no existe."
            );
        }

        if (!especialidad.get().isActivo()) {
            throw new SQLException(
                    "La especialidad seleccionada "
                            + "se encuentra inactiva."
            );
        }
    }

    private void verificarRelacionNoExistente(
            Connection conexion,
            long idMedico,
            int idEspecialidad
    ) throws SQLException {

        Optional<MedicoEspecialidad> relacionExistente =
                relacionDAO.buscarPorIds(
                        conexion,
                        idMedico,
                        idEspecialidad
                );

        if (relacionExistente.isPresent()) {
            throw new SQLException(
                    "El médico ya tiene registrada "
                            + "esta especialidad."
            );
        }
    }

    private void verificarPrincipalDisponible(
            Connection conexion,
            long idMedico
    ) throws SQLException {

        Optional<MedicoEspecialidad> principalExistente =
                relacionDAO.buscarPrincipalActiva(
                        conexion,
                        idMedico
                );

        if (principalExistente.isPresent()) {
            throw new SQLException(
                    "El médico ya posee una especialidad "
                            + "principal activa."
            );
        }
    }

    private static long convertirIdMedico(
            String idMedico
    ) throws SQLException {

        if (idMedico == null
                || idMedico.isBlank()) {

            throw new SQLException(
                    "El médico es obligatorio."
            );
        }

        try {
            long id =
                    Long.parseLong(
                            idMedico.strip()
                    );

            validarIdMedico(
                    id
            );

            return id;

        } catch (NumberFormatException error) {

            throw new SQLException(
                    "El médico seleccionado no es válido.",
                    error
            );
        }
    }

    private static int convertirIdEspecialidad(
            String idEspecialidad
    ) throws SQLException {

        if (idEspecialidad == null
                || idEspecialidad.isBlank()) {

            throw new SQLException(
                    "La especialidad es obligatoria."
            );
        }

        try {
            int id =
                    Integer.parseInt(
                            idEspecialidad.strip()
                    );

            validarIdEspecialidad(
                    id
            );

            return id;

        } catch (NumberFormatException error) {

            throw new SQLException(
                    "La especialidad seleccionada "
                            + "no es válida.",
                    error
            );
        }
    }

    private static boolean convertirPrincipal(
            String principal
    ) throws SQLException {

        if (principal == null
                || principal.isBlank()) {

            return false;
        }

        String valor =
                principal.strip()
                        .toLowerCase(
                                Locale.ROOT
                        );

        return switch (valor) {
            case "true",
                 "on",
                 "1",
                 "si",
                 "sí" -> true;

            case "false",
                 "off",
                 "0",
                 "no" -> false;

            default ->
                    throw new SQLException(
                            "El valor de especialidad principal "
                                    + "no es válido."
                    );
        };
    }

    private static LocalDate convertirFechaDesde(
            String fechaDesde
    ) throws SQLException {

        if (fechaDesde == null
                || fechaDesde.isBlank()) {

            return LocalDate.now();
        }

        try {
            return LocalDate.parse(
                    fechaDesde.strip()
            );

        } catch (DateTimeParseException error) {

            throw new SQLException(
                    "La fecha de inicio "
                            + "de la especialidad no es válida.",
                    error
            );
        }
    }

    private static void validarIdMedico(
            long idMedico
    ) throws SQLException {

        if (idMedico <= 0) {
            throw new SQLException(
                    "El ID del médico debe ser "
                            + "mayor que cero."
            );
        }
    }

    private static void validarIdEspecialidad(
            int idEspecialidad
    ) throws SQLException {

        if (idEspecialidad <= 0) {
            throw new SQLException(
                    "El ID de la especialidad debe ser "
                            + "mayor que cero."
            );
        }
    }

    private static void ejecutarRollback(
            Connection conexion,
            Exception errorOriginal
    ) {

        try {
            conexion.rollback();

        } catch (SQLException errorRollback) {

            errorOriginal.addSuppressed(
                    errorRollback
            );
        }
    }
}