package py.fpune.soracare.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EliminacionCuentaDAO {

    public boolean tieneCitasFuturasActivas(
            Connection conexion,
            long idPersona
    ) throws SQLException {

        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM soracare.pacientes paciente
                    JOIN soracare.citas cita
                      ON cita.id_paciente = paciente.id_paciente
                    JOIN soracare.estados_citas estado
                      ON estado.id_estado_cita = cita.id_estado_cita
                    WHERE paciente.id_persona = ?
                      AND cita.fecha_hora_inicio >= CURRENT_TIMESTAMP
                      AND estado.es_final = FALSE
                )
                """;

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPersona);

            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() && resultado.getBoolean(1);
            }
        }
    }

    public void eliminarIdentidadesExternas(
            Connection conexion,
            long idUsuario
    ) throws SQLException {

        String sql = """
                DELETE FROM soracare.identidades_externas
                WHERE id_usuario = ?
                """;

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idUsuario);
            sentencia.executeUpdate();
        }
    }

    public boolean inactivarUsuario(
            Connection conexion,
            long idUsuario,
            long idPersona
    ) throws SQLException {

        String sql = """
                UPDATE soracare.usuarios
                SET estado = 'INACTIVO',
                    intentos_fallidos = 0,
                    bloqueado_hasta = NULL,
                    debe_cambiar_contrasena = FALSE,
                    actualizado_en = CURRENT_TIMESTAMP
                WHERE id_usuario = ?
                  AND id_persona = ?
                  AND estado = 'ACTIVO'
                """;

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idUsuario);
            sentencia.setLong(2, idPersona);
            return sentencia.executeUpdate() == 1;
        }
    }
}
