package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.CitaAgendaMedicoDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AgendaMedicoDAO {

    private static final String BASE_SELECT = """
            SELECT
                c.id_cita,
                c.id_paciente,
                c.fecha_hora_inicio,
                c.fecha_hora_fin,
                CONCAT_WS(' ', per.nombres, per.apellidos)
                    AS paciente_nombre_completo,
                per.cedula AS cedula_paciente,
                pac.numero_historia_clinica,
                esp.nombre AS especialidad,
                con.nombre AS consultorio,
                c.motivo_consulta,
                c.observaciones,
                est.codigo AS codigo_estado,
                est.nombre AS nombre_estado
            FROM soracare.citas c
            INNER JOIN soracare.estados_citas est
                ON est.id_estado_cita = c.id_estado_cita
            INNER JOIN soracare.pacientes pac
                ON pac.id_paciente = c.id_paciente
            INNER JOIN soracare.personas per
                ON per.id_persona = pac.id_persona
            INNER JOIN soracare.especialidades esp
                ON esp.id_especialidad = c.id_especialidad
            INNER JOIN soracare.consultorios con
                ON con.id_consultorio = c.id_consultorio
            """;

    private static final String SQL_LISTAR_POR_MEDICO_Y_FECHA = BASE_SELECT + """
            WHERE c.id_medico = ?
              AND c.fecha_hora_inicio >= ?
              AND c.fecha_hora_inicio < ?
              AND est.codigo <> 'CANCELADA'
            ORDER BY c.fecha_hora_inicio, c.id_cita
            """;

    public List<CitaAgendaMedicoDTO> listarPorMedicoYFecha(
            long idMedico,
            OffsetDateTime desde,
            OffsetDateTime hasta
    ) throws SQLException {
        if (idMedico <= 0) {
            throw new IllegalArgumentException("El médico no es válido.");
        }
        if (desde == null || hasta == null || !hasta.isAfter(desde)) {
            throw new IllegalArgumentException("El rango de la agenda no es válido.");
        }

        List<CitaAgendaMedicoDTO> citas = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(
                     SQL_LISTAR_POR_MEDICO_Y_FECHA)) {
            sentencia.setLong(1, idMedico);
            sentencia.setObject(2, desde);
            sentencia.setObject(3, hasta);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    citas.add(mapear(resultado));
                }
            }
        }
        return citas;
    }

    public Optional<CitaAgendaMedicoDTO> buscarPorCitaYMedico(
            long idCita,
            long idMedico
    ) throws SQLException {
        if (idCita <= 0 || idMedico <= 0) {
            throw new IllegalArgumentException("La cita o el médico no son válidos.");
        }
        String sql = BASE_SELECT + """
                WHERE c.id_cita = ?
                  AND c.id_medico = ?
                  AND est.codigo <> 'CANCELADA'
                """;
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idCita);
            sentencia.setLong(2, idMedico);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next()
                        ? Optional.of(mapear(resultado))
                        : Optional.empty();
            }
        }
    }

    private static CitaAgendaMedicoDTO mapear(ResultSet resultado)
            throws SQLException {
        return new CitaAgendaMedicoDTO(
                resultado.getLong("id_cita"),
                resultado.getLong("id_paciente"),
                resultado.getObject("fecha_hora_inicio", OffsetDateTime.class),
                resultado.getObject("fecha_hora_fin", OffsetDateTime.class),
                resultado.getString("paciente_nombre_completo"),
                resultado.getString("cedula_paciente"),
                resultado.getString("numero_historia_clinica"),
                resultado.getString("especialidad"),
                resultado.getString("consultorio"),
                resultado.getString("motivo_consulta"),
                resultado.getString("observaciones"),
                resultado.getString("codigo_estado"),
                resultado.getString("nombre_estado")
        );
    }
}
