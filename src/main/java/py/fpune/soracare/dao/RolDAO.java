package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.mvc.model.Rol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;

public class RolDAO {

    private static final String SQL_BUSCAR_ACTIVO_POR_NOMBRE = """
            SELECT
                id_rol,
                nombre,
                descripcion,
                activo,
                creado_en
            FROM soracare.roles
            WHERE LOWER(nombre) = LOWER(?)
              AND activo = TRUE
            """;

    public Optional<Rol> buscarActivoPorNombre(
            String nombreRol
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return buscarActivoPorNombre(
                    conexion,
                    nombreRol
            );
        }
    }

    public Optional<Rol> buscarActivoPorNombre(
            Connection conexion,
            String nombreRol
    ) throws SQLException {

        if (conexion == null) {
            throw new SQLException(
                    "La conexión no puede ser nula."
            );
        }

        if (nombreRol == null || nombreRol.isBlank()) {
            throw new SQLException(
                    "El nombre del rol es obligatorio."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_BUSCAR_ACTIVO_POR_NOMBRE
                        )
        ) {
            sentencia.setString(
                    1,
                    nombreRol.strip()
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    return Optional.empty();
                }

                return Optional.of(
                        mapearRol(resultado)
                );
            }
        }
    }

    private static Rol mapearRol(
            ResultSet resultado
    ) throws SQLException {

        Rol rol = new Rol();

        rol.setIdRol(
                resultado.getShort("id_rol")
        );

        rol.setNombre(
                resultado.getString("nombre")
        );

        rol.setDescripcion(
                resultado.getString("descripcion")
        );

        rol.setActivo(
                resultado.getBoolean("activo")
        );

        rol.setCreadoEn(
                resultado.getObject(
                        "creado_en",
                        OffsetDateTime.class
                )
        );

        return rol;
    }
}