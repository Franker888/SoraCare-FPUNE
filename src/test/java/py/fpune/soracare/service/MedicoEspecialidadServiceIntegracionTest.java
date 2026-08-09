package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dao.EspecialidadDAO;
import py.fpune.soracare.dao.MedicoDAO;
import py.fpune.soracare.dao.PersonaDAO;
import py.fpune.soracare.dto.AsignacionMedicoEspecialidadDTO;
import py.fpune.soracare.mvc.model.Especialidad;
import py.fpune.soracare.mvc.model.EstadoMedico;
import py.fpune.soracare.mvc.model.Medico;
import py.fpune.soracare.mvc.model.MedicoEspecialidad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class MedicoEspecialidadServiceIntegracionTest {

    private static final String SQL_INACTIVAR_ESPECIALIDAD = """
            UPDATE soracare.especialidades
            SET activo = FALSE
            WHERE id_especialidad = ?
            """;

    private static final String SQL_ELIMINAR_RELACIONES = """
            DELETE FROM soracare.medicos_especialidades
            WHERE id_medico = ?
            """;

    private static final String SQL_ELIMINAR_MEDICO = """
            DELETE FROM soracare.medicos
            WHERE id_medico = ?
            """;

    private static final String SQL_ELIMINAR_ESPECIALIDAD = """
            DELETE FROM soracare.especialidades
            WHERE id_especialidad = ?
            """;

    private static final String SQL_ELIMINAR_PERSONA = """
            DELETE FROM soracare.personas
            WHERE id_persona = ?
            """;

    private static final String SQL_CONTAR_RELACIONES = """
            SELECT COUNT(*) AS cantidad
            FROM soracare.medicos_especialidades
            WHERE id_medico = ?
            """;

    public static void main(
            String[] args
    ) throws Exception {

        MedicoEspecialidadService servicio =
                new MedicoEspecialidadService();

        String identificadorTemporal =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8);

        Medico medico = null;

        Especialidad especialidadPrincipal = null;
        Especialidad especialidadSecundaria = null;
        Especialidad especialidadInactiva = null;

        try {
            DatosTemporales datosTemporales =
                    crearDatosTemporales(
                            identificadorTemporal
                    );

            medico =
                    datosTemporales.medico();

            especialidadPrincipal =
                    datosTemporales.especialidadPrincipal();

            especialidadSecundaria =
                    datosTemporales.especialidadSecundaria();

            especialidadInactiva =
                    datosTemporales.especialidadInactiva();

            probarSolicitudNula(
                    servicio
            );

            probarMedicoInvalido(
                    servicio,
                    especialidadPrincipal
            );

            probarEspecialidadInvalida(
                    servicio,
                    medico
            );

            probarMedicoInexistente(
                    servicio,
                    especialidadPrincipal
            );

            probarEspecialidadInexistente(
                    servicio,
                    medico
            );

            probarEspecialidadInactiva(
                    servicio,
                    medico,
                    especialidadInactiva
            );

            MedicoEspecialidad principal =
                    probarAsignacionPrincipal(
                            servicio,
                            medico,
                            especialidadPrincipal
                    );

            probarBusquedaPrincipal(
                    servicio,
                    medico,
                    especialidadPrincipal
            );

            probarRelacionDuplicada(
                    servicio,
                    medico,
                    especialidadPrincipal
            );

            probarSegundaPrincipal(
                    servicio,
                    medico,
                    especialidadSecundaria
            );

            MedicoEspecialidad secundaria =
                    probarAsignacionSecundaria(
                            servicio,
                            medico,
                            especialidadSecundaria
                    );

            probarListadoGeneral(
                    servicio,
                    medico,
                    principal,
                    secundaria
            );

            probarListadoActivas(
                    servicio,
                    medico,
                    principal,
                    secundaria
            );

            verificarCantidadRelaciones(
                    medico.getIdMedico(),
                    2
            );

            System.out.println();
            System.out.println(
                    "MedicoEspecialidadService "
                            + "fue probado correctamente."
            );

            System.out.println(
                    "La especialidad principal "
                            + "fue asignada correctamente."
            );

            System.out.println(
                    "La especialidad secundaria "
                            + "fue asignada correctamente."
            );

            System.out.println(
                    "La relación duplicada fue rechazada."
            );

            System.out.println(
                    "La segunda especialidad principal "
                            + "fue rechazada."
            );

            System.out.println(
                    "La especialidad inactiva "
                            + "fue rechazada."
            );

            System.out.println(
                    "Los médicos y especialidades "
                            + "inexistentes fueron rechazados."
            );

            System.out.println(
                    "Solo quedaron las dos relaciones "
                            + "válidas esperadas."
            );

        } finally {

            eliminarDatosTemporales(
                    medico,
                    especialidadPrincipal,
                    especialidadSecundaria,
                    especialidadInactiva
            );

            verificarEliminacion(
                    medico,
                    especialidadPrincipal,
                    especialidadSecundaria,
                    especialidadInactiva
            );

            System.out.println(
                    "Los datos temporales fueron eliminados. "
                            + "No quedaron datos de prueba."
            );
        }
    }

    private static DatosTemporales crearDatosTemporales(
            String identificador
    ) throws Exception {

        PersonaDAO personaDAO =
                new PersonaDAO();

        MedicoDAO medicoDAO =
                new MedicoDAO();

        EspecialidadDAO especialidadDAO =
                new EspecialidadDAO();

        Medico medico =
                crearMedicoTemporal(
                        identificador
                );

        Especialidad especialidadPrincipal =
                new Especialidad(
                        "Principal Service "
                                + identificador,
                        "Especialidad principal temporal.",
                        30
                );

        Especialidad especialidadSecundaria =
                new Especialidad(
                        "Secundaria Service "
                                + identificador,
                        "Especialidad secundaria temporal.",
                        40
                );

        Especialidad especialidadInactiva =
                new Especialidad(
                        "Inactiva Service "
                                + identificador,
                        "Especialidad inactiva temporal.",
                        30
                );

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            conexion.setAutoCommit(
                    false
            );

            try {
                personaDAO.registrar(
                        conexion,
                        medico
                );

                medicoDAO.registrar(
                        conexion,
                        medico
                );

                especialidadPrincipal =
                        especialidadDAO.crear(
                                conexion,
                                especialidadPrincipal
                        );

                especialidadSecundaria =
                        especialidadDAO.crear(
                                conexion,
                                especialidadSecundaria
                        );

                especialidadInactiva =
                        especialidadDAO.crear(
                                conexion,
                                especialidadInactiva
                        );

                try (
                        PreparedStatement sentencia =
                                conexion.prepareStatement(
                                        SQL_INACTIVAR_ESPECIALIDAD
                                )
                ) {
                    sentencia.setInt(
                            1,
                            especialidadInactiva
                                    .getIdEspecialidad()
                    );

                    sentencia.executeUpdate();
                }

                especialidadInactiva.setActivo(
                        false
                );

                conexion.commit();

            } catch (Exception error) {

                try {
                    conexion.rollback();

                } catch (SQLException errorRollback) {
                    error.addSuppressed(
                            errorRollback
                    );
                }

                throw error;
            }
        }

        System.out.println(
                "El médico temporal fue creado."
        );

        System.out.println(
                "Las especialidades temporales fueron creadas."
        );

        System.out.println(
                "Una especialidad temporal fue inactivada."
        );

        return new DatosTemporales(
                medico,
                especialidadPrincipal,
                especialidadSecundaria,
                especialidadInactiva
        );
    }

    private static Medico crearMedicoTemporal(
            String identificador
    ) {

        Medico medico =
                new Medico();

        medico.setNombres(
                "Médico"
        );

        medico.setApellidos(
                "Service "
                        + identificador
        );

        medico.setCedula(
                Long.toString(
                        generarCedulaTemporal()
                )
        );

        medico.setFechaNacimiento(
                LocalDate.of(
                        1982,
                        4,
                        10
                )
        );

        medico.setTelefono(
                "0981 000 002"
        );

        medico.setCorreo(
                "medico.relacion."
                        + identificador
                        .toLowerCase(Locale.ROOT)
                        + "@soracare.test"
        );

        medico.setDireccion(
                "Dirección temporal"
        );

        medico.setMatriculaProfesional(
                "MES-"
                        + identificador
                        .toUpperCase(Locale.ROOT)
        );

        medico.setFechaIngreso(
                LocalDate.now()
                        .minusYears(4)
        );

        medico.setEstado(
                EstadoMedico.ACTIVO
        );

        medico.setBiografia(
                "Médico temporal para prueba "
                        + "del servicio de especialidades."
        );

        return medico;
    }

    private static void probarSolicitudNula(
            MedicoEspecialidadService servicio
    ) {

        try {
            servicio.asignar(
                    null
            );

            throw new AssertionError(
                    "Una asignación nula "
                            + "debería ser rechazada."
            );

        } catch (SQLException esperado) {

            System.out.println(
                    "Solicitud nula: RECHAZADA."
            );
        }
    }

    private static void probarMedicoInvalido(
            MedicoEspecialidadService servicio,
            Especialidad especialidad
    ) {

        AsignacionMedicoEspecialidadDTO datos =
                new AsignacionMedicoEspecialidadDTO(
                        "0",
                        String.valueOf(
                                especialidad
                                        .getIdEspecialidad()
                        ),
                        "false",
                        ""
                );

        verificarAsignacionRechazada(
                servicio,
                datos,
                "ID de médico inválido"
        );
    }

    private static void probarEspecialidadInvalida(
            MedicoEspecialidadService servicio,
            Medico medico
    ) {

        AsignacionMedicoEspecialidadDTO datos =
                new AsignacionMedicoEspecialidadDTO(
                        String.valueOf(
                                medico.getIdMedico()
                        ),
                        "0",
                        "false",
                        ""
                );

        verificarAsignacionRechazada(
                servicio,
                datos,
                "ID de especialidad inválido"
        );
    }

    private static void probarMedicoInexistente(
            MedicoEspecialidadService servicio,
            Especialidad especialidad
    ) {

        AsignacionMedicoEspecialidadDTO datos =
                new AsignacionMedicoEspecialidadDTO(
                        String.valueOf(
                                Long.MAX_VALUE
                        ),
                        String.valueOf(
                                especialidad
                                        .getIdEspecialidad()
                        ),
                        "false",
                        ""
                );

        verificarAsignacionRechazada(
                servicio,
                datos,
                "Médico inexistente"
        );
    }

    private static void probarEspecialidadInexistente(
            MedicoEspecialidadService servicio,
            Medico medico
    ) {

        AsignacionMedicoEspecialidadDTO datos =
                new AsignacionMedicoEspecialidadDTO(
                        String.valueOf(
                                medico.getIdMedico()
                        ),
                        String.valueOf(
                                Short.MAX_VALUE
                        ),
                        "false",
                        ""
                );

        verificarAsignacionRechazada(
                servicio,
                datos,
                "Especialidad inexistente"
        );
    }

    private static void probarEspecialidadInactiva(
            MedicoEspecialidadService servicio,
            Medico medico,
            Especialidad especialidad
    ) {

        AsignacionMedicoEspecialidadDTO datos =
                new AsignacionMedicoEspecialidadDTO(
                        String.valueOf(
                                medico.getIdMedico()
                        ),
                        String.valueOf(
                                especialidad
                                        .getIdEspecialidad()
                        ),
                        "false",
                        ""
                );

        verificarAsignacionRechazada(
                servicio,
                datos,
                "Especialidad inactiva"
        );
    }

    private static MedicoEspecialidad probarAsignacionPrincipal(
            MedicoEspecialidadService servicio,
            Medico medico,
            Especialidad especialidad
    ) throws SQLException {

        AsignacionMedicoEspecialidadDTO datos =
                new AsignacionMedicoEspecialidadDTO(
                        String.valueOf(
                                medico.getIdMedico()
                        ),
                        String.valueOf(
                                especialidad
                                        .getIdEspecialidad()
                        ),
                        "on",
                        ""
                );

        MedicoEspecialidad relacion =
                servicio.asignar(
                        datos
                );

        if (relacion == null) {
            throw new AssertionError(
                    "El servicio no devolvió "
                            + "la relación principal."
            );
        }

        if (!relacion.isPrincipal()) {
            throw new AssertionError(
                    "La especialidad debería "
                            + "ser principal."
            );
        }

        if (!relacion.isActivo()) {
            throw new AssertionError(
                    "La relación principal "
                            + "debería estar activa."
            );
        }

        if (!LocalDate.now().equals(
                relacion.getFechaDesde()
        )) {
            throw new AssertionError(
                    "La fecha vacía debería utilizar "
                            + "la fecha actual."
            );
        }

        System.out.println(
                "Especialidad principal: ASIGNADA."
        );

        return relacion;
    }

    private static void probarBusquedaPrincipal(
            MedicoEspecialidadService servicio,
            Medico medico,
            Especialidad especialidadPrincipal
    ) throws SQLException {

        Optional<MedicoEspecialidad> resultado =
                servicio.buscarPrincipalActiva(
                        medico.getIdMedico()
                );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "No fue encontrada "
                            + "la especialidad principal."
            );
        }

        if (!especialidadPrincipal
                .getIdEspecialidad()
                .equals(
                        resultado.get()
                                .getIdEspecialidad()
                )) {

            throw new AssertionError(
                    "La especialidad principal "
                            + "recuperada no coincide."
            );
        }

        System.out.println(
                "La especialidad principal "
                        + "fue localizada."
        );
    }

    private static void probarRelacionDuplicada(
            MedicoEspecialidadService servicio,
            Medico medico,
            Especialidad especialidad
    ) {

        AsignacionMedicoEspecialidadDTO datos =
                new AsignacionMedicoEspecialidadDTO(
                        String.valueOf(
                                medico.getIdMedico()
                        ),
                        String.valueOf(
                                especialidad
                                        .getIdEspecialidad()
                        ),
                        "false",
                        LocalDate.now()
                                .toString()
                );

        verificarAsignacionRechazada(
                servicio,
                datos,
                "Relación duplicada"
        );
    }

    private static void probarSegundaPrincipal(
            MedicoEspecialidadService servicio,
            Medico medico,
            Especialidad especialidad
    ) {

        AsignacionMedicoEspecialidadDTO datos =
                new AsignacionMedicoEspecialidadDTO(
                        String.valueOf(
                                medico.getIdMedico()
                        ),
                        String.valueOf(
                                especialidad
                                        .getIdEspecialidad()
                        ),
                        "true",
                        LocalDate.now()
                                .minusYears(1)
                                .toString()
                );

        verificarAsignacionRechazada(
                servicio,
                datos,
                "Segunda especialidad principal"
        );
    }

    private static MedicoEspecialidad probarAsignacionSecundaria(
            MedicoEspecialidadService servicio,
            Medico medico,
            Especialidad especialidad
    ) throws SQLException {

        LocalDate fechaEsperada =
                LocalDate.now()
                        .minusYears(1);

        AsignacionMedicoEspecialidadDTO datos =
                new AsignacionMedicoEspecialidadDTO(
                        String.valueOf(
                                medico.getIdMedico()
                        ),
                        String.valueOf(
                                especialidad
                                        .getIdEspecialidad()
                        ),
                        "false",
                        fechaEsperada.toString()
                );

        MedicoEspecialidad relacion =
                servicio.asignar(
                        datos
                );

        if (relacion == null) {
            throw new AssertionError(
                    "El servicio no devolvió "
                            + "la relación secundaria."
            );
        }

        if (relacion.isPrincipal()) {
            throw new AssertionError(
                    "La especialidad secundaria "
                            + "no debería ser principal."
            );
        }

        if (!fechaEsperada.equals(
                relacion.getFechaDesde()
        )) {
            throw new AssertionError(
                    "La fecha de inicio "
                            + "no coincide."
            );
        }

        System.out.println(
                "Especialidad secundaria: ASIGNADA."
        );

        return relacion;
    }

    private static void probarListadoGeneral(
            MedicoEspecialidadService servicio,
            Medico medico,
            MedicoEspecialidad principal,
            MedicoEspecialidad secundaria
    ) throws SQLException {

        List<MedicoEspecialidad> relaciones =
                servicio.listarPorMedico(
                        medico.getIdMedico()
                );

        verificarRelacionesPresentes(
                relaciones,
                principal,
                secundaria,
                "listado general"
        );
    }

    private static void probarListadoActivas(
            MedicoEspecialidadService servicio,
            Medico medico,
            MedicoEspecialidad principal,
            MedicoEspecialidad secundaria
    ) throws SQLException {

        List<MedicoEspecialidad> relaciones =
                servicio.listarActivasPorMedico(
                        medico.getIdMedico()
                );

        verificarRelacionesPresentes(
                relaciones,
                principal,
                secundaria,
                "listado de activas"
        );

        if (relaciones.stream()
                .anyMatch(
                        relacion ->
                                !relacion.isActivo()
                )) {

            throw new AssertionError(
                    "El listado de activas contiene "
                            + "una relación inactiva."
            );
        }
    }

    private static void verificarRelacionesPresentes(
            List<MedicoEspecialidad> relaciones,
            MedicoEspecialidad principal,
            MedicoEspecialidad secundaria,
            String descripcion
    ) {

        boolean principalEncontrada =
                relaciones.stream()
                        .anyMatch(
                                relacion ->
                                        principal
                                                .getIdEspecialidad()
                                                .equals(
                                                        relacion
                                                                .getIdEspecialidad()
                                                )
                        );

        boolean secundariaEncontrada =
                relaciones.stream()
                        .anyMatch(
                                relacion ->
                                        secundaria
                                                .getIdEspecialidad()
                                                .equals(
                                                        relacion
                                                                .getIdEspecialidad()
                                                )
                        );

        if (!principalEncontrada
                || !secundariaEncontrada) {

            throw new AssertionError(
                    "No aparecen las dos relaciones "
                            + "esperadas en el "
                            + descripcion
                            + "."
            );
        }
    }

    private static void verificarAsignacionRechazada(
            MedicoEspecialidadService servicio,
            AsignacionMedicoEspecialidadDTO datos,
            String descripcion
    ) {

        try {
            servicio.asignar(
                    datos
            );

            throw new AssertionError(
                    descripcion
                            + " debería ser rechazada."
            );

        } catch (SQLException esperado) {

            System.out.println(
                    descripcion
                            + ": RECHAZADA."
            );
        }
    }

    private static void verificarCantidadRelaciones(
            long idMedico,
            int cantidadEsperada
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion();

                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_CONTAR_RELACIONES
                        )
        ) {
            sentencia.setLong(
                    1,
                    idMedico
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    throw new AssertionError(
                            "No fue posible contar "
                                    + "las relaciones."
                    );
                }

                int cantidad =
                        resultado.getInt(
                                "cantidad"
                        );

                if (cantidad != cantidadEsperada) {
                    throw new AssertionError(
                            "Se esperaban "
                                    + cantidadEsperada
                                    + " relaciones, pero existen "
                                    + cantidad
                                    + "."
                    );
                }
            }
        }
    }

    private static void eliminarDatosTemporales(
            Medico medico,
            Especialidad principal,
            Especialidad secundaria,
            Especialidad inactiva
    ) throws SQLException {

        if (medico == null) {
            return;
        }

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            conexion.setAutoCommit(
                    false
            );

            try {
                try (
                        PreparedStatement sentencia =
                                conexion.prepareStatement(
                                        SQL_ELIMINAR_RELACIONES
                                )
                ) {
                    sentencia.setLong(
                            1,
                            medico.getIdMedico()
                    );

                    sentencia.executeUpdate();
                }

                try (
                        PreparedStatement sentencia =
                                conexion.prepareStatement(
                                        SQL_ELIMINAR_MEDICO
                                )
                ) {
                    sentencia.setLong(
                            1,
                            medico.getIdMedico()
                    );

                    sentencia.executeUpdate();
                }

                eliminarEspecialidad(
                        conexion,
                        principal
                );

                eliminarEspecialidad(
                        conexion,
                        secundaria
                );

                eliminarEspecialidad(
                        conexion,
                        inactiva
                );

                try (
                        PreparedStatement sentencia =
                                conexion.prepareStatement(
                                        SQL_ELIMINAR_PERSONA
                                )
                ) {
                    sentencia.setLong(
                            1,
                            medico.getId()
                    );

                    sentencia.executeUpdate();
                }

                conexion.commit();

            } catch (SQLException error) {

                try {
                    conexion.rollback();

                } catch (SQLException errorRollback) {
                    error.addSuppressed(
                            errorRollback
                    );
                }

                throw error;
            }
        }
    }

    private static void eliminarEspecialidad(
            Connection conexion,
            Especialidad especialidad
    ) throws SQLException {

        if (especialidad == null
                || especialidad.getIdEspecialidad() == null) {

            return;
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_ELIMINAR_ESPECIALIDAD
                        )
        ) {
            sentencia.setInt(
                    1,
                    especialidad.getIdEspecialidad()
            );

            sentencia.executeUpdate();
        }
    }

    private static void verificarEliminacion(
            Medico medico,
            Especialidad principal,
            Especialidad secundaria,
            Especialidad inactiva
    ) throws SQLException {

        if (medico == null) {
            return;
        }

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            if (existeRegistro(
                    conexion,
                    "soracare.medicos",
                    "id_medico",
                    medico.getIdMedico()
            )) {
                throw new AssertionError(
                        "El médico temporal "
                                + "no fue eliminado."
                );
            }

            if (existeRegistro(
                    conexion,
                    "soracare.personas",
                    "id_persona",
                    medico.getId()
            )) {
                throw new AssertionError(
                        "La persona temporal "
                                + "no fue eliminada."
                );
            }

            verificarEspecialidadEliminada(
                    conexion,
                    principal
            );

            verificarEspecialidadEliminada(
                    conexion,
                    secundaria
            );

            verificarEspecialidadEliminada(
                    conexion,
                    inactiva
            );
        }
    }

    private static void verificarEspecialidadEliminada(
            Connection conexion,
            Especialidad especialidad
    ) throws SQLException {

        if (especialidad == null
                || especialidad.getIdEspecialidad() == null) {

            return;
        }

        if (existeRegistro(
                conexion,
                "soracare.especialidades",
                "id_especialidad",
                especialidad.getIdEspecialidad()
        )) {
            throw new AssertionError(
                    "Una especialidad temporal "
                            + "no fue eliminada."
            );
        }
    }

    private static boolean existeRegistro(
            Connection conexion,
            String tabla,
            String columna,
            long id
    ) throws SQLException {

        String sql =
                "SELECT EXISTS ("
                        + "SELECT 1 FROM "
                        + tabla
                        + " WHERE "
                        + columna
                        + " = ?"
                        + ") AS existe";

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                sql
                        )
        ) {
            sentencia.setLong(
                    1,
                    id
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                return resultado.next()
                        && resultado.getBoolean(
                        "existe"
                );
            }
        }
    }

    private static long generarCedulaTemporal() {

        return 5_000_000_000L
                + Math.abs(
                UUID.randomUUID()
                        .getMostSignificantBits()
                        % 900_000_000L
        );
    }

    private record DatosTemporales(
            Medico medico,
            Especialidad especialidadPrincipal,
            Especialidad especialidadSecundaria,
            Especialidad especialidadInactiva
    ) {
    }
}