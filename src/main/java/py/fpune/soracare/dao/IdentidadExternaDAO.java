package py.fpune.soracare.dao;

import py.fpune.soracare.dto.PerfilGoogleDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.OptionalLong;

public class IdentidadExternaDAO {

    private static final String PROVEEDOR_GOOGLE = "GOOGLE";

    private static final String SQL_BUSCAR_USUARIO = """
            SELECT id_usuario
            FROM soracare.identidades_externas
            WHERE proveedor = ?
              AND identificador_externo = ?
            """;

    private static final String SQL_REGISTRAR = """
            INSERT INTO soracare.identidades_externas (
                id_usuario,
                proveedor,
                identificador_externo,
                correo_proveedor,
                correo_verificado,
                nombre_mostrado,
                foto_url
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_ACTUALIZAR_ACCESO = """
            UPDATE soracare.identidades_externas
            SET correo_proveedor = ?,
                correo_verificado = ?,
                nombre_mostrado = ?,
                foto_url = ?,
                ultimo_acceso = CURRENT_TIMESTAMP
            WHERE proveedor = ?
              AND identificador_externo = ?
            """;

    public OptionalLong buscarIdUsuarioGoogle(
            Connection conexion,
            String identificadorGoogle
    ) throws SQLException {
        validarConexion(conexion);
        String identificador = textoObligatorio(
                identificadorGoogle,
                "El identificador de Google es obligatorio."
        );

        try (PreparedStatement sentencia = conexion.prepareStatement(
                SQL_BUSCAR_USUARIO)) {
            sentencia.setString(1, PROVEEDOR_GOOGLE);
            sentencia.setString(2, identificador);

            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next()
                        ? OptionalLong.of(resultado.getLong("id_usuario"))
                        : OptionalLong.empty();
            }
        }
    }

    public void registrarGoogle(
            Connection conexion,
            long idUsuario,
            PerfilGoogleDTO perfil
    ) throws SQLException {
        validarConexion(conexion);
        validarDatos(idUsuario, perfil);

        try (PreparedStatement sentencia = conexion.prepareStatement(
                SQL_REGISTRAR)) {
            sentencia.setLong(1, idUsuario);
            sentencia.setString(2, PROVEEDOR_GOOGLE);
            sentencia.setString(3, perfil.identificadorGoogle().strip());
            sentencia.setString(4, perfil.correo().strip().toLowerCase());
            sentencia.setBoolean(5, perfil.correoVerificado());
            sentencia.setString(6, textoOpcional(perfil.nombreCompleto()));
            sentencia.setString(7, textoOpcional(perfil.fotoUrl()));

            if (sentencia.executeUpdate() != 1) {
                throw new SQLException(
                        "No fue posible vincular la cuenta de Google."
                );
            }
        }
    }

    public void actualizarUltimoAccesoGoogle(
            Connection conexion,
            PerfilGoogleDTO perfil
    ) throws SQLException {
        validarConexion(conexion);
        validarDatos(1, perfil);

        try (PreparedStatement sentencia = conexion.prepareStatement(
                SQL_ACTUALIZAR_ACCESO)) {
            sentencia.setString(1, perfil.correo().strip().toLowerCase());
            sentencia.setBoolean(2, perfil.correoVerificado());
            sentencia.setString(3, textoOpcional(perfil.nombreCompleto()));
            sentencia.setString(4, textoOpcional(perfil.fotoUrl()));
            sentencia.setString(5, PROVEEDOR_GOOGLE);
            sentencia.setString(6, perfil.identificadorGoogle().strip());
            sentencia.executeUpdate();
        }
    }

    private static void validarDatos(
            long idUsuario,
            PerfilGoogleDTO perfil
    ) throws SQLException {
        if (idUsuario <= 0) {
            throw new SQLException("El usuario vinculado no es válido.");
        }
        if (perfil == null) {
            throw new SQLException("El perfil de Google es obligatorio.");
        }
        textoObligatorio(
                perfil.identificadorGoogle(),
                "El identificador de Google es obligatorio."
        );
        textoObligatorio(
                perfil.correo(),
                "El correo verificado por Google es obligatorio."
        );
    }

    private static String textoObligatorio(
            String valor,
            String mensaje
    ) throws SQLException {
        if (valor == null || valor.isBlank()) {
            throw new SQLException(mensaje);
        }
        return valor.strip();
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank()
                ? null
                : valor.strip();
    }

    private static void validarConexion(Connection conexion)
            throws SQLException {
        if (conexion == null) {
            throw new SQLException("La conexión no puede ser nula.");
        }
    }
}
