package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.mvc.model.Sede;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SedeDAO {

    private static final String SQL_INSERTAR = """
            INSERT INTO soracare.sedes (
                codigo,
                nombre,
                direccion,
                ciudad,
                departamento,
                telefono,
                correo,
                activo
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING
                id_sede,
                codigo,
                nombre,
                direccion,
                ciudad,
                departamento,
                telefono,
                correo,
                activo,
                fecha_creacion,
                fecha_actualizacion
            """;

    private static final String SQL_BUSCAR_POR_ID = """
            SELECT
                id_sede,
                codigo,
                nombre,
                direccion,
                ciudad,
                departamento,
                telefono,
                correo,
                activo,
                fecha_creacion,
                fecha_actualizacion
            FROM soracare.sedes
            WHERE id_sede = ?
            """;

    private static final String SQL_BUSCAR_POR_CODIGO = """
            SELECT
                id_sede,
                codigo,
                nombre,
                direccion,
                ciudad,
                departamento,
                telefono,
                correo,
                activo,
                fecha_creacion,
                fecha_actualizacion
            FROM soracare.sedes
            WHERE LOWER(BTRIM(codigo)) =
                  LOWER(BTRIM(?))
            """;

    private static final String SQL_BUSCAR_POR_NOMBRE = """
            SELECT
                id_sede,
                codigo,
                nombre,
                direccion,
                ciudad,
                departamento,
                telefono,
                correo,
                activo,
                fecha_creacion,
                fecha_actualizacion
            FROM soracare.sedes
            WHERE LOWER(BTRIM(nombre)) =
                  LOWER(BTRIM(?))
            """;

    private static final String SQL_LISTAR_TODAS = """
            SELECT
                id_sede,
                codigo,
                nombre,
                direccion,
                ciudad,
                departamento,
                telefono,
                correo,
                activo,
                fecha_creacion,
                fecha_actualizacion
            FROM soracare.sedes
            ORDER BY
                activo DESC,
                nombre ASC
            """;

    private static final String SQL_LISTAR_ACTIVAS = """
            SELECT
                id_sede,
                codigo,
                nombre,
                direccion,
                ciudad,
                departamento,
                telefono,
                correo,
                activo,
                fecha_creacion,
                fecha_actualizacion
            FROM soracare.sedes
            WHERE activo = TRUE
            ORDER BY nombre ASC
            """;

    public Sede crear(
            Sede sede
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return crear(
                    conexion,
                    sede
            );
        }
    }

    public Sede crear(
            Connection conexion,
            Sede sede
    ) throws SQLException {

        validarConexion(
                conexion
        );

        if (sede == null) {
            throw new SQLException(
                    "La sede es obligatoria."
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
                    sede.getCodigo()
            );

            sentencia.setString(
                    2,
                    sede.getNombre()
            );

            sentencia.setString(
                    3,
                    sede.getDireccion()
            );

            sentencia.setString(
                    4,
                    sede.getCiudad()
            );

            sentencia.setString(
                    5,
                    sede.getDepartamento()
            );

            sentencia.setString(
                    6,
                    sede.getTelefono()
            );

            sentencia.setString(
                    7,
                    sede.getCorreo()
            );

            sentencia.setBoolean(
                    8,
                    sede.isActivo()
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "No fue posible registrar la sede."
                    );
                }

                return mapearSede(
                        resultado
                );
            }
        }
    }

    public Optional<Sede> buscarPorId(
            int idSede
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return buscarPorId(
                    conexion,
                    idSede
            );
        }
    }

    public Optional<Sede> buscarPorId(
            Connection conexion,
            int idSede
    ) throws SQLException {

        validarConexion(
                conexion
        );

        if (idSede <= 0) {
            throw new SQLException(
                    "El ID de la sede debe ser mayor que cero."
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
                    idSede
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    return Optional.empty();
                }

                return Optional.of(
                        mapearSede(
                                resultado
                        )
                );
            }
        }
    }

    public Optional<Sede> buscarPorCodigo(
            String codigo
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return buscarPorCodigo(
                    conexion,
                    codigo
            );
        }
    }

    public Optional<Sede> buscarPorCodigo(
            Connection conexion,
            String codigo
    ) throws SQLException {

        validarConexion(
                conexion
        );

        if (codigo == null
                || codigo.isBlank()) {

            throw new SQLException(
                    "El código de la sede es obligatorio."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_BUSCAR_POR_CODIGO
                        )
        ) {
            sentencia.setString(
                    1,
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
                        mapearSede(
                                resultado
                        )
                );
            }
        }
    }

    public Optional<Sede> buscarPorNombre(
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

    public Optional<Sede> buscarPorNombre(
            Connection conexion,
            String nombre
    ) throws SQLException {

        validarConexion(
                conexion
        );

        if (nombre == null
                || nombre.isBlank()) {

            throw new SQLException(
                    "El nombre de la sede es obligatorio."
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
                        mapearSede(
                                resultado
                        )
                );
            }
        }
    }

    public List<Sede> listarTodas()
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

    public List<Sede> listarTodas(
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

    public List<Sede> listarActivas()
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

    public List<Sede> listarActivas(
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

    private static List<Sede> mapearLista(
            ResultSet resultado
    ) throws SQLException {

        List<Sede> sedes =
                new ArrayList<>();

        while (resultado.next()) {
            sedes.add(
                    mapearSede(
                            resultado
                    )
            );
        }

        return sedes;
    }

    private static Sede mapearSede(
            ResultSet resultado
    ) throws SQLException {

        Sede sede =
                new Sede();

        sede.setIdSede(
                resultado.getInt(
                        "id_sede"
                )
        );

        sede.setCodigo(
                resultado.getString(
                        "codigo"
                )
        );

        sede.setNombre(
                resultado.getString(
                        "nombre"
                )
        );

        sede.setDireccion(
                resultado.getString(
                        "direccion"
                )
        );

        sede.setCiudad(
                resultado.getString(
                        "ciudad"
                )
        );

        sede.setDepartamento(
                resultado.getString(
                        "departamento"
                )
        );

        sede.setTelefono(
                resultado.getString(
                        "telefono"
                )
        );

        sede.setCorreo(
                resultado.getString(
                        "correo"
                )
        );

        sede.setActivo(
                resultado.getBoolean(
                        "activo"
                )
        );

        sede.setFechaCreacion(
                resultado.getObject(
                        "fecha_creacion",
                        OffsetDateTime.class
                )
        );

        sede.setFechaActualizacion(
                resultado.getObject(
                        "fecha_actualizacion",
                        OffsetDateTime.class
                )
        );

        return sede;
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