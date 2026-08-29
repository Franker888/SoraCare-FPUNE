package py.fpune.soracare.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SuscripcionPushDAO {

    public void registrarOActualizar(
            Connection conexion,
            long idUsuario,
            String token,
            String userAgent
    ) throws SQLException {
        String sql = """
                INSERT INTO soracare.suscripciones_push (
                    id_usuario, token, user_agent, activa,
                    creado_en, actualizado_en, ultima_actividad_en
                ) VALUES (?, ?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (token) DO UPDATE
                    SET id_usuario = EXCLUDED.id_usuario,
                        user_agent = EXCLUDED.user_agent,
                        activa = TRUE,
                        actualizado_en = CURRENT_TIMESTAMP,
                        ultima_actividad_en = CURRENT_TIMESTAMP
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idUsuario);
            sentencia.setString(2, token);
            sentencia.setString(3, userAgent);
            sentencia.executeUpdate();
        }
    }

    public void desactivar(
            Connection conexion,
            long idUsuario,
            String token
    ) throws SQLException {
        String sql = """
                UPDATE soracare.suscripciones_push
                   SET activa = FALSE,
                       actualizado_en = CURRENT_TIMESTAMP
                 WHERE id_usuario = ?
                   AND token = ?
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idUsuario);
            sentencia.setString(2, token);
            sentencia.executeUpdate();
        }
    }

    public int contarActivas(Connection conexion, long idUsuario) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                  FROM soracare.suscripciones_push
                 WHERE id_usuario = ?
                   AND activa = TRUE
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idUsuario);
            try (ResultSet resultado = sentencia.executeQuery()) {
                resultado.next();
                return resultado.getInt(1);
            }
        }
    }
}
