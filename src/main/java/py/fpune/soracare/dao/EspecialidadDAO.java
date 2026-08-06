package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.mvc.model.Especialidad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EspecialidadDAO {

    private static final String SQL_INSERTAR = """
            INSERT INTO soracare.especialidades (
                nombre,
                descripcion,
                duracion_consulta_minutos,
                activo
            )
            VALUES (?, ?, ?, ?)
            RETURNING
                id_especialidad,
                nombre,
                descripcion,
                duracion_consulta_minutos,
                activo,
                fecha_creacion,
                fecha_actualizacion
            """;

    private static final String SQL_BUSCAR_POR_ID = """
            SELECT
                id_especialidad,
                nombre,
                descripcion,
                duracion_consulta_minutos,
                activo,
                fecha_creacion,
                fecha_actualizacion
            FROM soracare.especialidades
            WHERE id_especialidad = ?
            """;

    private static final String SQL_BUSCAR_POR_NOMBRE = """
            SELECT
                id_especialidad,
                nombre,
                descripcion,
                duracion_consulta_minutos,
                activo,
                fecha_creacion,
                fecha_actualizacion
            FROM soracare.especialidades
            WHERE LOWER(BTRIM(nombre)) =
                  LOWER(BTRIM(?))
            """;

    private static final String SQL_LISTAR_TODAS = """
            SELECT
                id_especialidad,
                nombre,
                descripcion,
                duracion_consulta_minutos,
                activo,
                fecha_creacion,
                fecha_actualizacion
            FROM soracare.especialidades
            ORDER BY
                activo DESC,
                nombre ASC
            """;

    private static final String SQL_LISTAR_ACTIVAS = """
            SELECT
                id_especialidad,
                nombre,
                descripcion,
                duracion_consulta_minutos,
                activo,
                fecha_creacion,
                fecha_actualizacion
            FROM soracare.especialidades
            WHERE activo = TRUE
            ORDER BY nombre ASC
            """;

    public Especialidad crear(
            Especialidad especialidad
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return crear(
                    conexion,
                    especialidad
            );
        }
    }

    public Especialidad crear(
            Connection conexion,
            Especialidad especialidad
    ) throws SQLException {

        validarConexion(
                conexion
        );

        if (especialidad == null) {
            throw new SQLException(
                    "La especialidad es obligatoria."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_INSERTAR
                        )
        ) {
            sentencia.setString(
                    1,
                    especialidad.getNombre()
            );

            sentencia.setString(
                    2,
                    especialidad.getDescripcion()
            );

            sentencia.setInt(
                    3,
                    especialidad
                            .getDuracionConsultaMinutos()
            );

            sentencia.setBoolean(
                    4,
                    especialidad.isActivo()
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "No fue posible registrar "
                                    + "la especialidad."
                    );
                }

                return mapearEspecialidad(
                        resultado
                );
            }
        }
    }

    public Optional<Especialidad> buscarPorId(
            int idEspecialidad
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return buscarPorId(
                    conexion,
                    idEspecialidad
            );
        }
    }

    public Optional<Especialidad> buscarPorId(
            Connection conexion,
            int idEspecialidad
    ) throws SQLException {

        validarConexion(
                conexion
        );

        if (idEspecialidad <= 0) {
            throw new SQLException(
                    "El ID de la especialidad "
                            + "debe ser mayor que cero."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_BUSCAR_POR_ID
                        )
        ) {
            sentencia.setInt(
                    1,
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
                        mapearEspecialidad(
                                resultado
                        )
                );
            }
        }
    }

    public Optional<Especialidad> buscarPorNombre(
            String nombre
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return buscarPorNombre(
                    conexion,
                    nombre
            );
        }
    }

    public Optional<Especialidad> buscarPorNombre(
            Connection conexion,
            String nombre
    ) throws SQLException {

        validarConexion(
                conexion
        );

        if (nombre == null
                || nombre.isBlank()) {

            throw new SQLException(
                    "El nombre de la especialidad "
                            + "es obligatorio."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_BUSCAR_POR_NOMBRE
                        )
        ) {
            sentencia.setString(
                    1,
                    nombre.strip()
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    return Optional.empty();
                }

                return Optional.of(
                        mapearEspecialidad(
                                resultado
                        )
                );
            }
        }
    }

    public List<Especialidad> listarTodas()
            throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return listarTodas(
                    conexion
            );
        }
    }

    public List<Especialidad> listarTodas(
            Connection conexion
    ) throws SQLException {

        validarConexion(
                conexion
        );

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_LISTAR_TODAS
                        );

                ResultSet resultado =
                        sentencia.executeQuery()
        ) {
            return mapearLista(
                    resultado
            );
        }
    }

    public List<Especialidad> listarActivas()
            throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return listarActivas(
                    conexion
            );
        }
    }

    public List<Especialidad> listarActivas(
            Connection conexion
    ) throws SQLException {

        validarConexion(
                conexion
        );

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_LISTAR_ACTIVAS
                        );

                ResultSet resultado =
                        sentencia.executeQuery()
        ) {
            return mapearLista(
                    resultado
            );
        }
    }

    private static List<Especialidad> mapearLista(
            ResultSet resultado
    ) throws SQLException {

        List<Especialidad> especialidades =
                new ArrayList<>();

        while (resultado.next()) {
            especialidades.add(
                    mapearEspecialidad(
                            resultado
                    )
            );
        }

        return especialidades;
    }

    private static Especialidad mapearEspecialidad(
            ResultSet resultado
    ) throws SQLException {

        Especialidad especialidad =
                new Especialidad();

        especialidad.setIdEspecialidad(
                resultado.getInt(
                        "id_especialidad"
                )
        );

        especialidad.setNombre(
                resultado.getString(
                        "nombre"
                )
        );

        especialidad.setDescripcion(
                resultado.getString(
                        "descripcion"
                )
        );

        especialidad.setDuracionConsultaMinutos(
                resultado.getInt(
                        "duracion_consulta_minutos"
                )
        );

        especialidad.setActivo(
                resultado.getBoolean(
                        "activo"
                )
        );

        especialidad.setFechaCreacion(
                resultado.getObject(
                        "fecha_creacion",
                        OffsetDateTime.class
                )
        );

        especialidad.setFechaActualizacion(
                resultado.getObject(
                        "fecha_actualizacion",
                        OffsetDateTime.class
                )
        );

        return especialidad;
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