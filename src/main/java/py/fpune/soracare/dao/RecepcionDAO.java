package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.ResumenPanelRecepcionDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RecepcionDAO {

    private static final String SQL_RESUMEN = """
            SELECT
                (SELECT COUNT(*)
                   FROM soracare.citas cita
                   INNER JOIN soracare.estados_citas estado
                           ON estado.id_estado_cita = cita.id_estado_cita
                  WHERE (cita.fecha_hora_inicio AT TIME ZONE 'America/Asuncion')::DATE
                            = (CURRENT_TIMESTAMP AT TIME ZONE 'America/Asuncion')::DATE
                    AND estado.codigo NOT IN ('CANCELADA', 'AUSENTE')) AS citas_hoy,
                (SELECT COUNT(*)
                   FROM soracare.citas cita
                   INNER JOIN soracare.estados_citas estado
                           ON estado.id_estado_cita = cita.id_estado_cita
                  WHERE (cita.fecha_hora_inicio AT TIME ZONE 'America/Asuncion')::DATE
                            = (CURRENT_TIMESTAMP AT TIME ZONE 'America/Asuncion')::DATE
                    AND estado.codigo = 'CONFIRMADA') AS confirmadas_hoy,
                (SELECT COUNT(*)
                   FROM soracare.citas cita
                   INNER JOIN soracare.estados_citas estado
                           ON estado.id_estado_cita = cita.id_estado_cita
                  WHERE (cita.fecha_hora_inicio AT TIME ZONE 'America/Asuncion')::DATE
                            = (CURRENT_TIMESTAMP AT TIME ZONE 'America/Asuncion')::DATE
                    AND estado.codigo IN ('SOLICITADA', 'PENDIENTE_PAGO')) AS por_confirmar_hoy,
                (SELECT COUNT(DISTINCT pago.id_cita)
                   FROM soracare.pagos pago
                   INNER JOIN soracare.estados_pago estado_pago
                           ON estado_pago.id_estado_pago = pago.id_estado_pago
                   INNER JOIN soracare.citas cita ON cita.id_cita = pago.id_cita
                  INNER JOIN soracare.estados_citas estado_cita
                           ON estado_cita.id_estado_cita = cita.id_estado_cita
                  WHERE estado_pago.codigo = 'PENDIENTE'
                    AND (cita.fecha_hora_inicio AT TIME ZONE 'America/Asuncion')::DATE
                            >= (CURRENT_TIMESTAMP AT TIME ZONE 'America/Asuncion')::DATE
                    AND estado_cita.codigo NOT IN ('CANCELADA', 'AUSENTE')) AS cobros_pendientes
            """;

    public ResumenPanelRecepcionDTO consultarResumen() throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_RESUMEN);
             ResultSet resultado = sentencia.executeQuery()) {

            if (!resultado.next()) {
                throw new SQLException("No fue posible resumir la operación de recepción.");
            }

            return new ResumenPanelRecepcionDTO(
                    resultado.getLong("citas_hoy"),
                    resultado.getLong("confirmadas_hoy"),
                    resultado.getLong("por_confirmar_hoy"),
                    resultado.getLong("cobros_pendientes")
            );
        }
    }
}
