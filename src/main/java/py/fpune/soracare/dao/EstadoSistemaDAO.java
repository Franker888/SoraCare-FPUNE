package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.ResumenPanelAdministracionDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class EstadoSistemaDAO {

    private static final String SQL_ESTADO = """
            SELECT CURRENT_TIMESTAMP AS hora_base_datos,
                   current_setting('server_version') AS version_postgresql,
                   (SELECT COUNT(*) FROM soracare.usuarios WHERE estado = 'ACTIVO') AS usuarios_activos,
                   (SELECT COUNT(*)
                    FROM soracare.citas cit
                    INNER JOIN soracare.estados_citas est
                        ON est.id_estado_cita = cit.id_estado_cita
                    WHERE cit.fecha_hora_inicio >= CURRENT_TIMESTAMP
                      AND est.codigo NOT IN ('CANCELADA', 'AUSENTE')) AS citas_futuras,
                   (SELECT COUNT(*) FROM soracare.auditoria_eventos
                    WHERE fecha_evento >= CURRENT_TIMESTAMP - INTERVAL '24 hours') AS eventos_24h
            """;

    private static final String SQL_PANEL = """
            SELECT
                (SELECT COUNT(*) FROM soracare.usuarios) AS usuarios,
                (SELECT COUNT(*) FROM soracare.usuarios WHERE estado = 'BLOQUEADO') AS usuarios_bloqueados,
                (SELECT COUNT(*) FROM soracare.pacientes) AS pacientes,
                (SELECT COUNT(*) FROM soracare.medicos WHERE estado = 'ACTIVO') AS medicos,
                (SELECT COUNT(*) FROM soracare.medicamentos WHERE activo = TRUE) AS medicamentos,
                (SELECT COUNT(*)
                 FROM soracare.citas cit
                 INNER JOIN soracare.estados_citas est
                     ON est.id_estado_cita = cit.id_estado_cita
                 WHERE cit.fecha_hora_inicio >= CURRENT_TIMESTAMP
                   AND est.codigo NOT IN ('CANCELADA', 'AUSENTE')) AS citas_futuras,
                (SELECT COUNT(*) FROM soracare.auditoria_eventos
                 WHERE fecha_evento >= CURRENT_DATE) AS eventos_hoy
            """;

    public DatosBaseDatos consultarEstado() throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_ESTADO);
             ResultSet resultado = sentencia.executeQuery()) {
            if (!resultado.next()) throw new SQLException("PostgreSQL no devolvió su estado.");
            return new DatosBaseDatos(
                    resultado.getObject("hora_base_datos", OffsetDateTime.class),
                    resultado.getString("version_postgresql"),
                    resultado.getLong("usuarios_activos"),
                    resultado.getLong("citas_futuras"),
                    resultado.getLong("eventos_24h")
            );
        }
    }

    public ResumenPanelAdministracionDTO consultarResumenPanel() throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_PANEL);
             ResultSet resultado = sentencia.executeQuery()) {
            if (!resultado.next()) throw new SQLException("No fue posible resumir la administración.");
            return new ResumenPanelAdministracionDTO(
                    resultado.getLong("usuarios"),
                    resultado.getLong("usuarios_bloqueados"),
                    resultado.getLong("pacientes"),
                    resultado.getLong("medicos"),
                    resultado.getLong("medicamentos"),
                    resultado.getLong("citas_futuras"),
                    resultado.getLong("eventos_hoy")
            );
        }
    }

    public record DatosBaseDatos(
            OffsetDateTime hora,
            String version,
            long usuariosActivos,
            long citasFuturas,
            long eventosUltimas24Horas
    ) {
    }
}
