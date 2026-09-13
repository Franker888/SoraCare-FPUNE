package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import java.sql.Connection;
import java.sql.SQLException;

public class SesionVigenteDAO {
    public boolean esVigente(long idUsuario, long versionSesion) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            return esVigente(conexion, idUsuario, versionSesion);
        }
    }

    public boolean esVigente(Connection conexion, long idUsuario, long versionSesion)
            throws SQLException {
        try (var sentencia = conexion.prepareStatement("""
                SELECT EXISTS (SELECT 1 FROM soracare.usuarios
                    WHERE id_usuario = ? AND estado = 'ACTIVO' AND version_sesion = ?)
                """)) {
            sentencia.setLong(1, idUsuario);
            sentencia.setLong(2, versionSesion);
            try (var resultado = sentencia.executeQuery()) {
                return resultado.next() && resultado.getBoolean(1);
            }
        }
    }
}
