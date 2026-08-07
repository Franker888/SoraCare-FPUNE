package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.mvc.model.Consultorio;
import py.fpune.soracare.mvc.model.EstadoConsultorio;
import py.fpune.soracare.mvc.model.TipoConsultorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConsultorioDAO {

    private static final String SQL_INSERTAR = """
            INSERT INTO soracare.consultorios (
                id_sede,
                codigo,
                nombre,
                piso,
                ubicacion_referencia,
                tipo,
                capacidad,
                accesible,
                estado
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING
                id_consultorio,
                id_sede,
                codigo,
                nombre,
                piso,
                ubicacion_referencia,
                tipo,
                capacidad,
                accesible,
                estado,
                fecha_creacion,
                fecha_actualizacion
            """;

    private static final String SQL_BUSCAR_POR_ID = """
            SELECT
                id_consultorio,
                id_sede,
                codigo,
                nombre,
                piso,
                ubicacion_referencia,
                tipo,
                capacidad,
                accesible,
                estado,
                fecha_creacion,
                fecha_actualizacion
            FROM soracare.consultorios
            WHERE id_consultorio = ?
            """;

    private static final String SQL_BUSCAR_POR_SEDE_Y_CODIGO = """
            SELECT
                id_consultorio,
                id_sede,
                codigo,
                nombre,
                piso,
                ubicacion_referencia,
                tipo,
                capacidad,
                accesible,
                estado,
                fecha_creacion,
                fecha_actualizacion
            FROM soracare.consultorios
            WHERE id_sede = ?
              AND LOWER(BTRIM(codigo)) =
                  LOWER(BTRIM(?))
            """;

    private static final String SQL_LISTAR_TODOS = """
            SELECT
                id_consultorio,
                id_sede,
                codigo,
                nombre,
                piso,
                ubicacion_referencia,
                tipo,
                capacidad,
                accesible,
                estado,
                fecha_creacion,
                fecha_actualizacion
            FROM soracare.consultorios
            ORDER BY
                id_sede ASC,
                nombre ASC
            """;

    private static final String SQL_LISTAR_POR_SEDE = """
            SELECT
                id_consultorio,
                id_sede,
                codigo,
                nombre,
                piso,
                ubicacion_referencia,
                tipo,
                capacidad,
                accesible,
                estado,
                fecha_creacion,
                fecha_actualizacion
            FROM soracare.consultorios
            WHERE id_sede = ?
            ORDER BY
                estado ASC,
                nombre ASC
            """;

    private static final String SQL_LISTAR_DISPONIBLES_POR_SEDE = """
            SELECT
                id_consultorio,
                id_sede,
                codigo,
                nombre,
                piso,
                ubicacion_referencia,
                tipo,
                capacidad,
                accesible,
                estado,
                fecha_creacion,
                fecha_actualizacion
            FROM soracare.consultorios
            WHERE id_sede = ?
              AND estado = 'DISPONIBLE'
            ORDER BY nombre ASC
            """;

    public Consultorio crear(
            Consultorio consultorio
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return crear(
                    conexion,
                    consultorio
            );
        }
    }

    public Consultorio crear(
            Connection conexion,
            Consultorio consultorio
    ) throws SQLException {

        validarConexion(
                conexion
        );

        if (consultorio == null) {
            throw new SQLException(
                    "El consultorio es obligatorio."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_INSERTAR
                        )
        ) {
            sentencia.setInt(
                    1,
                    consultorio.getIdSede()
            );

            sentencia.setString(
                    2,
                    consultorio.getCodigo()
            );

            sentencia.setString(
                    3,
                    consultorio.getNombre()
            );

            sentencia.setString(
                    4,
                    consultorio.getPiso()
            );

            sentencia.setString(
                    5,
                    consultorio.getUbicacionReferencia()
            );

            sentencia.setString(
                    6,
                    consultorio
                            .getTipo()
                            .name()
            );

            sentencia.setShort(
                    7,
                    consultorio.getCapacidad()
            );

            sentencia.setBoolean(
                    8,
                    consultorio.isAccesible()
            );

            sentencia.setString(
                    9,
                    consultorio
                            .getEstado()
                            .name()
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "No fue posible registrar "
                                    + "el consultorio."
                    );
                }

                return mapearConsultorio(
                        resultado
                );
            }
        }
    }

    public Optional<Consultorio> buscarPorId(
            long idConsultorio
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return buscarPorId(
                    conexion,
                    idConsultorio
            );
        }
    }

    public Optional<Consultorio> buscarPorId(
            Connection conexion,
            long idConsultorio
    ) throws SQLException {

        validarConexion(
                conexion
        );

        if (idConsultorio <= 0) {
            throw new SQLException(
                    "El ID del consultorio debe ser "
                            + "mayor que cero."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_BUSCAR_POR_ID
                        )
        ) {
            sentencia.setLong(
                    1,
                    idConsultorio
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    return Optional.empty();
                }

                return Optional.of(
                        mapearConsultorio(
                                resultado
                        )
                );
            }
        }
    }

    public Optional<Consultorio> buscarPorSedeYCodigo(
            int idSede,
            String codigo
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return buscarPorSedeYCodigo(
                    conexion,
                    idSede,
                    codigo
            );
        }
    }

    public Optional<Consultorio> buscarPorSedeYCodigo(
            Connection conexion,
            int idSede,
            String codigo
    ) throws SQLException {

        validarConexion(
                conexion
        );

        if (idSede <= 0) {
            throw new SQLException(
                    "El ID de la sede debe ser "
                            + "mayor que cero."
            );
        }

        if (codigo == null
                || codigo.isBlank()) {

            throw new SQLException(
                    "El código del consultorio "
                            + "es obligatorio."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_BUSCAR_POR_SEDE_Y_CODIGO
                        )
        ) {
            sentencia.setInt(
                    1,
                    idSede
            );

            sentencia.setString(
                    2,
                    codigo.strip()
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    return Optional.empty();
                }

                return Optional.of(
                        mapearConsultorio(
                                resultado
                        )
                );
            }
        }
    }

    public List<Consultorio> listarTodos()
            throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return listarTodos(
                    conexion
            );
        }
    }

    public List<Consultorio> listarTodos(
            Connection conexion
    ) throws SQLException {

        validarConexion(
                conexion
        );

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_LISTAR_TODOS
                        );

                ResultSet resultado =
                        sentencia.executeQuery()
        ) {
            return mapearLista(
                    resultado
            );
        }
    }

    public List<Consultorio> listarPorSede(
            int idSede
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return listarPorSede(
                    conexion,
                    idSede
            );
        }
    }

    public List<Consultorio> listarPorSede(
            Connection conexion,
            int idSede
    ) throws SQLException {

        validarConexion(
                conexion
        );

        if (idSede <= 0) {
            throw new SQLException(
                    "El ID de la sede debe ser "
                            + "mayor que cero."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_LISTAR_POR_SEDE
                        )
        ) {
            sentencia.setInt(
                    1,
                    idSede
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                return mapearLista(
                        resultado
                );
            }
        }
    }

    public List<Consultorio> listarDisponiblesPorSede(
            int idSede
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return listarDisponiblesPorSede(
                    conexion,
                    idSede
            );
        }
    }

    public List<Consultorio> listarDisponiblesPorSede(
            Connection conexion,
            int idSede
    ) throws SQLException {

        validarConexion(
                conexion
        );

        if (idSede <= 0) {
            throw new SQLException(
                    "El ID de la sede debe ser "
                            + "mayor que cero."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_LISTAR_DISPONIBLES_POR_SEDE
                        )
        ) {
            sentencia.setInt(
                    1,
                    idSede
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                return mapearLista(
                        resultado
                );
            }
        }
    }

    private static List<Consultorio> mapearLista(
            ResultSet resultado
    ) throws SQLException {

        List<Consultorio> consultorios =
                new ArrayList<>();

        while (resultado.next()) {
            consultorios.add(
                    mapearConsultorio(
                            resultado
                    )
            );
        }

        return consultorios;
    }

    private static Consultorio mapearConsultorio(
            ResultSet resultado
    ) throws SQLException {

        Consultorio consultorio =
                new Consultorio();

        consultorio.setIdConsultorio(
                resultado.getLong(
                        "id_consultorio"
                )
        );

        consultorio.setIdSede(
                resultado.getInt(
                        "id_sede"
                )
        );

        consultorio.setCodigo(
                resultado.getString(
                        "codigo"
                )
        );

        consultorio.setNombre(
                resultado.getString(
                        "nombre"
                )
        );

        consultorio.setPiso(
                resultado.getString(
                        "piso"
                )
        );

        consultorio.setUbicacionReferencia(
                resultado.getString(
                        "ubicacion_referencia"
                )
        );

        consultorio.setTipo(
                TipoConsultorio.valueOf(
                        resultado.getString(
                                "tipo"
                        )
                )
        );

        consultorio.setCapacidad(
                resultado.getShort(
                        "capacidad"
                )
        );

        consultorio.setAccesible(
                resultado.getBoolean(
                        "accesible"
                )
        );

        consultorio.setEstado(
                EstadoConsultorio.valueOf(
                        resultado.getString(
                                "estado"
                        )
                )
        );

        consultorio.setFechaCreacion(
                resultado.getObject(
                        "fecha_creacion",
                        OffsetDateTime.class
                )
        );

        consultorio.setFechaActualizacion(
                resultado.getObject(
                        "fecha_actualizacion",
                        OffsetDateTime.class
                )
        );

        return consultorio;
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