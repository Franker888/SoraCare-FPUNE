package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.ResumenPanelPacienteDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;

public class PanelPacienteDAO {

    private static final String SQL_RESUMEN = """
            SELECT pac.id_paciente,
                   per.nombres,
                   per.apellidos,
                   pac.numero_historia_clinica,
                   pac.grupo_sanguineo,
                   (SELECT COUNT(*)
                      FROM soracare.citas cita
                      INNER JOIN soracare.estados_citas estado
                              ON estado.id_estado_cita = cita.id_estado_cita
                     WHERE cita.id_paciente = pac.id_paciente
                       AND cita.fecha_hora_fin >= CURRENT_TIMESTAMP
                       AND estado.codigo IN ('SOLICITADA', 'PENDIENTE_PAGO', 'CONFIRMADA')
                   ) AS proximas_citas,
                   (SELECT COUNT(*)
                      FROM soracare.recetas_medicas receta
                     WHERE receta.id_paciente = pac.id_paciente
                       AND receta.estado <> 'ANULADA'
                   ) AS indicaciones_medicas,
                   (SELECT COUNT(*)
                      FROM soracare.documentos documento
                     WHERE documento.id_paciente = pac.id_paciente
                       AND documento.estado = 'DISPONIBLE'
                   ) AS documentos_clinicos,
                   (SELECT COUNT(*)
                      FROM soracare.notificaciones notificacion
                      INNER JOIN soracare.canales_notificacion canal
                              ON canal.id_canal_notificacion = notificacion.id_canal_notificacion
                     WHERE notificacion.id_persona = per.id_persona
                       AND canal.codigo = 'SISTEMA'
                       AND notificacion.fecha_lectura IS NULL
                   ) AS notificaciones_no_leidas,
                   (SELECT MIN(cita.fecha_hora_inicio)
                      FROM soracare.citas cita
                      INNER JOIN soracare.estados_citas estado
                              ON estado.id_estado_cita = cita.id_estado_cita
                     WHERE cita.id_paciente = pac.id_paciente
                       AND cita.fecha_hora_fin >= CURRENT_TIMESTAMP
                       AND estado.codigo IN ('SOLICITADA', 'PENDIENTE_PAGO', 'CONFIRMADA')
                   ) AS proxima_cita
              FROM soracare.pacientes pac
              INNER JOIN soracare.personas per ON per.id_persona = pac.id_persona
             WHERE pac.id_persona = ?
            """;

    public Optional<ResumenPanelPacienteDTO> consultarResumen(long idPersona)
            throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_RESUMEN)) {
            sentencia.setLong(1, idPersona);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    return Optional.empty();
                }
                return Optional.of(new ResumenPanelPacienteDTO(
                        resultado.getLong("id_paciente"),
                        resultado.getString("nombres"),
                        resultado.getString("apellidos"),
                        resultado.getString("numero_historia_clinica"),
                        resultado.getString("grupo_sanguineo"),
                        resultado.getLong("proximas_citas"),
                        resultado.getLong("indicaciones_medicas"),
                        resultado.getLong("documentos_clinicos"),
                        resultado.getLong("notificaciones_no_leidas"),
                        resultado.getObject("proxima_cita", OffsetDateTime.class)
                ));
            }
        }
    }
}
