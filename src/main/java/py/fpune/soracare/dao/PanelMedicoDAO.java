package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.ResumenPanelMedicoDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class PanelMedicoDAO {

    private static final String SQL_RESUMEN = """
            SELECT med.id_medico,
                   per.nombres,
                   per.apellidos,
                   med.tratamiento,
                   med.matricula_profesional,
                   COALESCE((
                       SELECT esp.nombre
                         FROM soracare.medicos_especialidades me
                         INNER JOIN soracare.especialidades esp
                                 ON esp.id_especialidad = me.id_especialidad
                        WHERE me.id_medico = med.id_medico
                          AND me.activo = TRUE
                        ORDER BY me.es_principal DESC, esp.nombre
                        LIMIT 1
                   ), 'Sin especialidad activa') AS especialidad_principal,
                   (SELECT COUNT(*)
                      FROM soracare.citas cita
                      INNER JOIN soracare.estados_citas estado
                              ON estado.id_estado_cita = cita.id_estado_cita
                     WHERE cita.id_medico = med.id_medico
                       AND (cita.fecha_hora_inicio AT TIME ZONE 'America/Asuncion')::DATE
                               = (CURRENT_TIMESTAMP AT TIME ZONE 'America/Asuncion')::DATE
                       AND estado.codigo NOT IN ('CANCELADA', 'AUSENTE')) AS citas_hoy,
                   (SELECT COUNT(*)
                      FROM soracare.citas cita
                      INNER JOIN soracare.estados_citas estado
                              ON estado.id_estado_cita = cita.id_estado_cita
                     WHERE cita.id_medico = med.id_medico
                       AND (cita.fecha_hora_inicio AT TIME ZONE 'America/Asuncion')::DATE
                               = (CURRENT_TIMESTAMP AT TIME ZONE 'America/Asuncion')::DATE
                       AND estado.codigo = 'CONFIRMADA') AS confirmadas_hoy,
                   (SELECT COUNT(*)
                      FROM soracare.citas cita
                      INNER JOIN soracare.estados_citas estado
                              ON estado.id_estado_cita = cita.id_estado_cita
                     WHERE cita.id_medico = med.id_medico
                       AND (cita.fecha_hora_inicio AT TIME ZONE 'America/Asuncion')::DATE
                               = (CURRENT_TIMESTAMP AT TIME ZONE 'America/Asuncion')::DATE
                       AND estado.codigo = 'EN_ATENCION') AS en_atencion_hoy,
                   (SELECT COUNT(*)
                      FROM soracare.citas cita
                      INNER JOIN soracare.estados_citas estado
                              ON estado.id_estado_cita = cita.id_estado_cita
                     WHERE cita.id_medico = med.id_medico
                       AND DATE_TRUNC('month', cita.fecha_hora_inicio AT TIME ZONE 'America/Asuncion')
                               = DATE_TRUNC('month', CURRENT_TIMESTAMP AT TIME ZONE 'America/Asuncion')
                       AND estado.codigo = 'ATENDIDA') AS atendidas_mes
              FROM soracare.medicos med
              INNER JOIN soracare.personas per ON per.id_persona = med.id_persona
             WHERE med.id_persona = ? AND med.estado = 'ACTIVO'
            """;

    public Optional<ResumenPanelMedicoDTO> consultarResumen(long idPersona)
            throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_RESUMEN)) {
            sentencia.setLong(1, idPersona);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    return Optional.empty();
                }
                return Optional.of(new ResumenPanelMedicoDTO(
                        resultado.getLong("id_medico"),
                        resultado.getString("nombres"),
                        resultado.getString("apellidos"),
                        resultado.getString("tratamiento"),
                        resultado.getString("matricula_profesional"),
                        resultado.getString("especialidad_principal"),
                        resultado.getLong("citas_hoy"),
                        resultado.getLong("confirmadas_hoy"),
                        resultado.getLong("en_atencion_hoy"),
                        resultado.getLong("atendidas_mes")
                ));
            }
        }
    }
}
