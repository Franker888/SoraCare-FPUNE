package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.mvc.model.Persona;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class PersonaDAO {

    private static final String SQL_EXISTE_POR_ID = """
            SELECT EXISTS (
                SELECT 1
                FROM soracare.personas
                WHERE id_persona = ?
            ) AS existe
            """;

    private static final String SQL_EXISTE_CEDULA = """
            SELECT EXISTS (
                SELECT 1
                FROM soracare.personas
                WHERE cedula = ?
            ) AS existe
            """;

    private static final String SQL_EXISTE_CORREO = """
            SELECT EXISTS (
                SELECT 1
                FROM soracare.personas
                WHERE LOWER(correo) = LOWER(?)
            ) AS existe
            """;

    private static final String SQL_REGISTRAR_PERSONA = """
            INSERT INTO soracare.personas (
                nombres,
                apellidos,
                cedula,
                fecha_nacimiento,
                telefono,
                correo,
                direccion
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id_persona
            """;

    public boolean existePorId(
            long idPersona
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return existePorId(
                    conexion,
                    idPersona
            );
        }
    }

    public boolean existePorId(
            Connection conexion,
            long idPersona
    ) throws SQLException {

        if (conexion == null) {
            throw new SQLException(
                    "La conexión no puede ser nula."
            );
        }

        if (idPersona <= 0) {
            throw new SQLException(
                    "El ID de la persona debe ser mayor que cero."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_EXISTE_POR_ID
                        )
        ) {
            sentencia.setLong(
                    1,
                    idPersona
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "No se pudo comprobar la existencia de la persona."
                    );
                }

                return resultado.getBoolean(
                        "existe"
                );
            }
        }
    }

    public boolean existeCedula(
            long cedula
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return existeCedula(
                    conexion,
                    cedula
            );
        }
    }

    public boolean existeCedula(
            Connection conexion,
            long cedula
    ) throws SQLException {

        if (conexion == null) {
            throw new SQLException(
                    "La conexión no puede ser nula."
            );
        }

        if (cedula <= 0) {
            throw new SQLException(
                    "La cédula debe ser mayor que cero."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_EXISTE_CEDULA
                        )
        ) {
            sentencia.setLong(
                    1,
                    cedula
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "No se pudo comprobar la cédula."
                    );
                }

                return resultado.getBoolean(
                        "existe"
                );
            }
        }
    }

    public boolean existeCorreo(
            String correo
    ) throws SQLException {

        if (correo == null || correo.isBlank()) {
            return false;
        }

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return existeCorreo(
                    conexion,
                    correo
            );
        }
    }

    public boolean existeCorreo(
            Connection conexion,
            String correo
    ) throws SQLException {

        if (conexion == null) {
            throw new SQLException(
                    "La conexión no puede ser nula."
            );
        }

        if (correo == null || correo.isBlank()) {
            return false;
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_EXISTE_CORREO
                        )
        ) {
            sentencia.setString(
                    1,
                    correo.strip()
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "No se pudo comprobar el correo electrónico."
                    );
                }

                return resultado.getBoolean(
                        "existe"
                );
            }
        }
    }

    public long registrar(
            Connection conexion,
            Persona persona
    ) throws SQLException {

        if (conexion == null) {
            throw new SQLException(
                    "La conexión no puede ser nula."
            );
        }

        if (persona == null) {
            throw new SQLException(
                    "La persona no puede ser nula."
            );
        }

        long cedulaNumerica =
                convertirCedula(
                        persona.getCedula()
                );

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_REGISTRAR_PERSONA
                        )
        ) {
            sentencia.setString(
                    1,
                    normalizarTextoObligatorio(
                            persona.getNombres(),
                            "nombres"
                    )
            );

            sentencia.setString(
                    2,
                    normalizarTextoObligatorio(
                            persona.getApellidos(),
                            "apellidos"
                    )
            );

            sentencia.setLong(
                    3,
                    cedulaNumerica
            );

            if (persona.getFechaNacimiento() == null) {
                sentencia.setNull(
                        4,
                        Types.DATE
                );
            } else {
                sentencia.setDate(
                        4,
                        Date.valueOf(
                                persona.getFechaNacimiento()
                        )
                );
            }

            establecerTextoOpcional(
                    sentencia,
                    5,
                    persona.getTelefono()
            );

            establecerTextoOpcional(
                    sentencia,
                    6,
                    persona.getCorreo()
            );

            establecerTextoOpcional(
                    sentencia,
                    7,
                    persona.getDireccion()
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "PostgreSQL no devolvió el ID de la persona."
                    );
                }

                long idGenerado =
                        resultado.getLong(
                                "id_persona"
                        );

                persona.setId(
                        idGenerado
                );

                return idGenerado;
            }
        }
    }

    private static long convertirCedula(
            String cedula
    ) throws SQLException {

        if (cedula == null || cedula.isBlank()) {
            throw new SQLException(
                    "La cédula es obligatoria."
            );
        }

        String cedulaLimpia =
                cedula.strip();

        if (!cedulaLimpia.matches("\\d+")) {
            throw new SQLException(
                    "La cédula debe contener solamente números."
            );
        }

        try {
            long cedulaNumerica =
                    Long.parseLong(
                            cedulaLimpia
                    );

            if (cedulaNumerica <= 0) {
                throw new SQLException(
                        "La cédula debe ser mayor que cero."
                );
            }

            return cedulaNumerica;

        } catch (NumberFormatException error) {
            throw new SQLException(
                    "El número de cédula no es válido.",
                    error
            );
        }
    }

    private static String normalizarTextoObligatorio(
            String valor,
            String campo
    ) throws SQLException {

        if (valor == null || valor.isBlank()) {
            throw new SQLException(
                    "El campo "
                            + campo
                            + " es obligatorio."
            );
        }

        return valor.strip();
    }

    private static void establecerTextoOpcional(
            PreparedStatement sentencia,
            int posicion,
            String valor
    ) throws SQLException {

        if (valor == null || valor.isBlank()) {
            sentencia.setNull(
                    posicion,
                    Types.VARCHAR
            );
        } else {
            sentencia.setString(
                    posicion,
                    valor.strip()
            );
        }
    }
}