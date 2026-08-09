package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.mvc.model.MedicoEspecialidad;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MedicoEspecialidadDAO {

    private static final String SQL_INSERTAR = """
            INSERT INTO soracare.medicos_especialidades (
                id_medico,
                id_especialidad,
                es_principal,
                fecha_desde,
                fecha_hasta,
                activo
            )
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING
                id_medico,
                id_especialidad,
                es_principal,
                fecha_desde,
                fecha_hasta,
                activo,
                fecha_creacion
            """;

    private static final String SQL_BUSCAR_POR_IDS = """
            SELECT
                id_medico,
                id_especialidad,
                es_principal,
                fecha_desde,
                fecha_hasta,
                activo,
                fecha_creacion
            FROM soracare.medicos_especialidades
            WHERE id_medico = ?
              AND id_especialidad = ?
            """;

    private static final String SQL_BUSCAR_PRINCIPAL_ACTIVA = """
            SELECT
                id_medico,
                id_especialidad,
                es_principal,
                fecha_desde,
                fecha_hasta,
                activo,
                fecha_creacion
            FROM soracare.medicos_especialidades
            WHERE id_medico = ?
              AND es_principal = TRUE
              AND activo = TRUE
            """;

    private static final String SQL_LISTAR_POR_MEDICO = """
            SELECT
                id_medico,
                id_especialidad,
                es_principal,
                fecha_desde,
                fecha_hasta,
                activo,
                fecha_creacion
            FROM soracare.medicos_especialidades
            WHERE id_medico = ?
            ORDER BY
                activo DESC,
                es_principal DESC,
                fecha_desde DESC,
                id_especialidad ASC
            """;

    private static final String SQL_LISTAR_ACTIVAS_POR_MEDICO = """
            SELECT
                id_medico,
                id_especialidad,
                es_principal,
                fecha_desde,
                fecha_hasta,
                activo,
                fecha_creacion
            FROM soracare.medicos_especialidades
            WHERE id_medico = ?
              AND activo = TRUE
            ORDER BY
                es_principal DESC,
                fecha_desde DESC,
                id_especialidad ASC
            """;

    public MedicoEspecialidad crear(
            MedicoEspecialidad relacion
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return crear(
                    conexion,
                    relacion
            );
        }
    }

    public MedicoEspecialidad crear(
            Connection conexion,
            MedicoEspecialidad relacion
    ) throws SQLException {

        validarConexion(
                conexion
        );

        if (relacion == null) {
            throw new SQLException(
                    "La relación médico-especialidad "
                            + "es obligatoria."
            );
        }

        if (relacion.getIdMedico() == null
                || relacion.getIdMedico() <= 0) {

            throw new SQLException(
                    "El ID del médico debe ser mayor que cero."
            );
        }

        if (relacion.getIdEspecialidad() == null
                || relacion.getIdEspecialidad() <= 0) {

            throw new SQLException(
                    "El ID de la especialidad "
                            + "debe ser mayor que cero."
            );
        }

        if (relacion.getFechaDesde() == null) {
            throw new SQLException(
                    "La fecha de inicio es obligatoria."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_INSERTAR
                        )
        ) {
            sentencia.setLong(
                    1,
                    relacion.getIdMedico()
            );

            sentencia.setInt(
                    2,
                    relacion.getIdEspecialidad()
            );

            sentencia.setBoolean(
                    3,
                    relacion.isPrincipal()
            );

            sentencia.setDate(
                    4,
                    Date.valueOf(
                            relacion.getFechaDesde()
                    )
            );

            if (relacion.getFechaHasta() == null) {
                sentencia.setNull(
                        5,
                        Types.DATE
                );
            } else {
                sentencia.setDate(
                        5,
                        Date.valueOf(
                                relacion.getFechaHasta()
                        )
                );
            }

            sentencia.setBoolean(
                    6,
                    relacion.isActivo()
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "No fue posible registrar "
                                    + "la especialidad del médico."
                    );
                }

                return mapearRelacion(
                        resultado
                );
            }
        }
    }

    public Optional<MedicoEspecialidad> buscarPorIds(
            long idMedico,
            int idEspecialidad
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return buscarPorIds(
                    conexion,
                    idMedico,
                    idEspecialidad
            );
        }
    }

    public Optional<MedicoEspecialidad> buscarPorIds(
            Connection conexion,
            long idMedico,
            int idEspecialidad
    ) throws SQLException {

        validarConexion(
                conexion
        );

        validarIds(
                idMedico,
                idEspecialidad
        );

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_BUSCAR_POR_IDS
                        )
        ) {
            sentencia.setLong(
                    1,
                    idMedico
            );

            sentencia.setInt(
                    2,
                    idEspecialidad
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    return Optional.empty();
                }

                return Optional.of(
                        mapearRelacion(
                                resultado
                        )
                );
            }
        }
    }

    public Optional<MedicoEspecialidad> buscarPrincipalActiva(
            long idMedico
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return buscarPrincipalActiva(
                    conexion,
                    idMedico
            );
        }
    }

    public Optional<MedicoEspecialidad> buscarPrincipalActiva(
            Connection conexion,
            long idMedico
    ) throws SQLException {

        validarConexion(
                conexion
        );

        validarIdMedico(
                idMedico
        );

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_BUSCAR_PRINCIPAL_ACTIVA
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
                    return Optional.empty();
                }

                return Optional.of(
                        mapearRelacion(
                                resultado
                        )
                );
            }
        }
    }

    public List<MedicoEspecialidad> listarPorMedico(
            long idMedico
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return listarPorMedico(
                    conexion,
                    idMedico
            );
        }
    }

    public List<MedicoEspecialidad> listarPorMedico(
            Connection conexion,
            long idMedico
    ) throws SQLException {

        validarConexion(
                conexion
        );

        validarIdMedico(
                idMedico
        );

        return ejecutarListadoPorMedico(
                conexion,
                SQL_LISTAR_POR_MEDICO,
                idMedico
        );
    }

    public List<MedicoEspecialidad> listarActivasPorMedico(
            long idMedico
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return listarActivasPorMedico(
                    conexion,
                    idMedico
            );
        }
    }

    public List<MedicoEspecialidad> listarActivasPorMedico(
            Connection conexion,
            long idMedico
    ) throws SQLException {

        validarConexion(
                conexion
        );

        validarIdMedico(
                idMedico
        );

        return ejecutarListadoPorMedico(
                conexion,
                SQL_LISTAR_ACTIVAS_POR_MEDICO,
                idMedico
        );
    }

    private static List<MedicoEspecialidad> ejecutarListadoPorMedico(
            Connection conexion,
            String sql,
            long idMedico
    ) throws SQLException {

        List<MedicoEspecialidad> relaciones =
                new ArrayList<>();

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                sql
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
                while (resultado.next()) {
                    relaciones.add(
                            mapearRelacion(
                                    resultado
                            )
                    );
                }
            }
        }

        return relaciones;
    }

    private static MedicoEspecialidad mapearRelacion(
            ResultSet resultado
    ) throws SQLException {

        MedicoEspecialidad relacion =
                new MedicoEspecialidad();

        relacion.setIdMedico(
                resultado.getLong(
                        "id_medico"
                )
        );

        relacion.setIdEspecialidad(
                resultado.getInt(
                        "id_especialidad"
                )
        );

        relacion.setPrincipal(
                resultado.getBoolean(
                        "es_principal"
                )
        );

        Date fechaDesde =
                resultado.getDate(
                        "fecha_desde"
                );

        if (fechaDesde != null) {
            relacion.setFechaDesde(
                    fechaDesde.toLocalDate()
            );
        }

        Date fechaHasta =
                resultado.getDate(
                        "fecha_hasta"
                );

        if (fechaHasta != null) {
            relacion.setFechaHasta(
                    fechaHasta.toLocalDate()
            );
        }

        relacion.setActivo(
                resultado.getBoolean(
                        "activo"
                )
        );

        relacion.setFechaCreacion(
                resultado.getObject(
                        "fecha_creacion",
                        OffsetDateTime.class
                )
        );

        return relacion;
    }

    private static void validarIds(
            long idMedico,
            int idEspecialidad
    ) throws SQLException {

        validarIdMedico(
                idMedico
        );

        if (idEspecialidad <= 0) {
            throw new SQLException(
                    "El ID de la especialidad "
                            + "debe ser mayor que cero."
            );
        }
    }

    private static void validarIdMedico(
            long idMedico
    ) throws SQLException {

        if (idMedico <= 0) {
            throw new SQLException(
                    "El ID del médico debe ser mayor que cero."
            );
        }
    }

    private static void validarConexion(
            Connection conexion
    ) throws SQLException {

        if (conexion == null) {
            throw new SQLException(
                    "La conexión no puede ser nula."
            );
        }

        if (conexion.isClosed()) {
            throw new SQLException(
                    "La conexión se encuentra cerrada."
            );
        }
    }
}