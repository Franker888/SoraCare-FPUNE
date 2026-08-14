package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.SlotDisponibleDTO;
import py.fpune.soracare.mvc.model.Cita;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CitaDAO {

    private static final String COLUMNAS = """
            c.id_cita, c.id_paciente, c.id_medico, c.id_especialidad,
            c.id_consultorio, c.id_horario, c.id_estado_cita,
            e.codigo AS codigo_estado, e.nombre AS nombre_estado,
            c.fecha_hora_inicio, c.fecha_hora_fin, c.motivo_consulta,
            c.observaciones, c.canal_reserva, c.motivo_cancelacion,
            c.id_usuario_creador, c.id_usuario_modificacion,
            c.fecha_creacion, c.fecha_actualizacion
            """;

    private static final String BASE_SELECT = "SELECT " + COLUMNAS + """
            FROM soracare.citas c
            INNER JOIN soracare.estados_citas e
                ON e.id_estado_cita = c.id_estado_cita
            """;

    private static final String SQL_INSERTAR = """
            INSERT INTO soracare.citas (
                id_paciente, id_medico, id_especialidad, id_consultorio,
                id_horario, id_estado_cita, fecha_hora_inicio, fecha_hora_fin,
                motivo_consulta, observaciones, canal_reserva, id_usuario_creador
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id_cita
            """;

    public Cita crear(Connection conexion, Cita cita) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR)) {
            sentencia.setLong(1, cita.getIdPaciente());
            sentencia.setLong(2, cita.getIdMedico());
            sentencia.setInt(3, cita.getIdEspecialidad());
            sentencia.setLong(4, cita.getIdConsultorio());
            if (cita.getIdHorario() == null) sentencia.setNull(5, Types.BIGINT);
            else sentencia.setLong(5, cita.getIdHorario());
            sentencia.setInt(6, cita.getIdEstadoCita());
            sentencia.setObject(7, cita.getFechaHoraInicio());
            sentencia.setObject(8, cita.getFechaHoraFin());
            sentencia.setString(9, cita.getMotivoConsulta());
            if (cita.getObservaciones() == null) sentencia.setNull(10, Types.VARCHAR);
            else sentencia.setString(10, cita.getObservaciones());
            sentencia.setString(11, cita.getCanalReserva());
            if (cita.getIdUsuarioCreador() == null) sentencia.setNull(12, Types.BIGINT);
            else sentencia.setLong(12, cita.getIdUsuarioCreador());
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) throw new SQLException("PostgreSQL no devolvió la cita creada.");
                cita.setIdCita(resultado.getLong("id_cita"));
            }
        }
        return buscarPorId(conexion, cita.getIdCita())
                .orElseThrow(() -> new SQLException("No fue posible recuperar la cita creada."));
    }

    public Optional<Cita> buscarPorId(Connection conexion, long idCita) throws SQLException {
        String sql = BASE_SELECT + " WHERE c.id_cita = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idCita);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        }
    }

    public Optional<Cita> buscarPorIdParaActualizar(
            Connection conexion,
            long idCita
    ) throws SQLException {
        String sql = BASE_SELECT + " WHERE c.id_cita = ? FOR UPDATE OF c";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idCita);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next()
                        ? Optional.of(mapear(resultado))
                        : Optional.empty();
            }
        }
    }

    public Optional<Cita> buscarPorId(long idCita) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            return buscarPorId(conexion, idCita);
        }
    }

    public List<Cita> listarEntre(OffsetDateTime desde, OffsetDateTime hasta)
            throws SQLException {
        String sql = BASE_SELECT + """
                WHERE c.fecha_hora_inicio >= ? AND c.fecha_hora_inicio < ?
                ORDER BY c.fecha_hora_inicio, c.id_medico, c.id_cita
                """;
        List<Cita> citas = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setObject(1, desde);
            sentencia.setObject(2, hasta);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) citas.add(mapear(resultado));
            }
        }
        return citas;
    }

    public List<Cita> listarProximas(int limite) throws SQLException {
        String sql = BASE_SELECT + """
                WHERE c.fecha_hora_fin >= CURRENT_TIMESTAMP
                ORDER BY c.fecha_hora_inicio, c.id_cita
                LIMIT ?
                """;
        List<Cita> citas = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, limite);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) citas.add(mapear(resultado));
            }
        }
        return citas;
    }

    public List<Cita> listarPorPaciente(long idPaciente, int limite) throws SQLException {
        String sql = BASE_SELECT + """
                WHERE c.id_paciente = ?
                ORDER BY c.fecha_hora_inicio DESC, c.id_cita DESC
                LIMIT ?
                """;
        List<Cita> citas = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPaciente);
            sentencia.setInt(2, limite);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) citas.add(mapear(resultado));
            }
        }
        return citas;
    }

    public boolean existeSuperposicion(
            Connection conexion,
            long idPaciente,
            long idMedico,
            long idConsultorio,
            OffsetDateTime inicio,
            OffsetDateTime fin
    ) throws SQLException {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM soracare.citas c
                    INNER JOIN soracare.estados_citas e
                        ON e.id_estado_cita = c.id_estado_cita
                    WHERE e.codigo <> 'CANCELADA'
                      AND ? < c.fecha_hora_fin AND ? > c.fecha_hora_inicio
                      AND (c.id_paciente = ? OR c.id_medico = ? OR c.id_consultorio = ?)
                ) AS existe
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setObject(1, inicio);
            sentencia.setObject(2, fin);
            sentencia.setLong(3, idPaciente);
            sentencia.setLong(4, idMedico);
            sentencia.setLong(5, idConsultorio);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() && resultado.getBoolean("existe");
            }
        }
    }

    public void cancelar(
            Connection conexion,
            long idCita,
            int idEstadoCancelada,
            long idUsuario,
            String motivo
    ) throws SQLException {
        String sql = """
                UPDATE soracare.citas
                SET id_estado_cita = ?, id_usuario_modificacion = ?, motivo_cancelacion = ?
                WHERE id_cita = ?
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idEstadoCancelada);
            sentencia.setLong(2, idUsuario);
            sentencia.setString(3, motivo);
            sentencia.setLong(4, idCita);
            if (sentencia.executeUpdate() != 1) throw new SQLException("No fue posible cancelar la cita.");
        }
    }

    public void actualizarEstado(
            Connection conexion,
            long idCita,
            int idEstado,
            long idUsuario
    ) throws SQLException {
        String sql = """
                UPDATE soracare.citas
                SET id_estado_cita = ?, id_usuario_modificacion = ?
                WHERE id_cita = ?
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idEstado);
            sentencia.setLong(2, idUsuario);
            sentencia.setLong(3, idCita);
            if (sentencia.executeUpdate() != 1) {
                throw new SQLException("No fue posible actualizar el estado de la cita.");
            }
        }
    }

    public Cita reprogramar(
            Connection conexion,
            Cita citaAnterior,
            SlotDisponibleDTO nuevoSlot,
            long idUsuario,
            String motivo
    ) throws SQLException {
        OffsetDateTime nuevoInicio = nuevoSlot.fecha().atTime(nuevoSlot.horaInicio())
                .atZone(java.time.ZoneId.of("America/Asuncion")).toOffsetDateTime();
        OffsetDateTime nuevoFin = nuevoSlot.fecha().atTime(nuevoSlot.horaFin())
                .atZone(java.time.ZoneId.of("America/Asuncion")).toOffsetDateTime();

        String sqlHistorial = """
                INSERT INTO soracare.reprogramaciones_citas (
                    id_cita, fecha_hora_inicio_anterior, fecha_hora_fin_anterior,
                    id_medico_anterior, id_especialidad_anterior,
                    id_consultorio_anterior, id_horario_anterior,
                    fecha_hora_inicio_nueva, fecha_hora_fin_nueva,
                    id_medico_nuevo, id_especialidad_nueva,
                    id_consultorio_nuevo, id_horario_nuevo,
                    motivo, id_usuario
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sqlHistorial)) {
            sentencia.setLong(1, citaAnterior.getIdCita());
            sentencia.setObject(2, citaAnterior.getFechaHoraInicio());
            sentencia.setObject(3, citaAnterior.getFechaHoraFin());
            sentencia.setLong(4, citaAnterior.getIdMedico());
            sentencia.setInt(5, citaAnterior.getIdEspecialidad());
            sentencia.setLong(6, citaAnterior.getIdConsultorio());
            if (citaAnterior.getIdHorario() == null) sentencia.setNull(7, Types.BIGINT);
            else sentencia.setLong(7, citaAnterior.getIdHorario());
            sentencia.setObject(8, nuevoInicio);
            sentencia.setObject(9, nuevoFin);
            sentencia.setLong(10, nuevoSlot.idMedico());
            sentencia.setInt(11, nuevoSlot.idEspecialidad());
            sentencia.setLong(12, nuevoSlot.idConsultorio());
            if (nuevoSlot.idHorario() == null) sentencia.setNull(13, Types.BIGINT);
            else sentencia.setLong(13, nuevoSlot.idHorario());
            sentencia.setString(14, motivo);
            sentencia.setLong(15, idUsuario);
            sentencia.executeUpdate();
        }

        String sqlCita = """
                UPDATE soracare.citas
                   SET id_medico = ?, id_especialidad = ?, id_consultorio = ?,
                       id_horario = ?, fecha_hora_inicio = ?, fecha_hora_fin = ?,
                       id_usuario_modificacion = ?, motivo_cancelacion = NULL
                 WHERE id_cita = ?
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sqlCita)) {
            sentencia.setLong(1, nuevoSlot.idMedico());
            sentencia.setInt(2, nuevoSlot.idEspecialidad());
            sentencia.setLong(3, nuevoSlot.idConsultorio());
            if (nuevoSlot.idHorario() == null) sentencia.setNull(4, Types.BIGINT);
            else sentencia.setLong(4, nuevoSlot.idHorario());
            sentencia.setObject(5, nuevoInicio);
            sentencia.setObject(6, nuevoFin);
            sentencia.setLong(7, idUsuario);
            sentencia.setLong(8, citaAnterior.getIdCita());
            if (sentencia.executeUpdate() != 1) {
                throw new SQLException("No fue posible reprogramar la cita.");
            }
        }
        return buscarPorId(conexion, citaAnterior.getIdCita())
                .orElseThrow(() -> new SQLException("No fue posible recuperar la cita reprogramada."));
    }

    private static Cita mapear(ResultSet resultado) throws SQLException {
        Cita cita = new Cita();
        cita.setIdCita(resultado.getLong("id_cita"));
        cita.setIdPaciente(resultado.getLong("id_paciente"));
        cita.setIdMedico(resultado.getLong("id_medico"));
        cita.setIdEspecialidad(resultado.getInt("id_especialidad"));
        cita.setIdConsultorio(resultado.getLong("id_consultorio"));
        Object idHorario = resultado.getObject("id_horario");
        if (idHorario != null) cita.setIdHorario(((Number) idHorario).longValue());
        cita.setIdEstadoCita(resultado.getInt("id_estado_cita"));
        cita.setCodigoEstado(resultado.getString("codigo_estado"));
        cita.setNombreEstado(resultado.getString("nombre_estado"));
        cita.setFechaHoraInicio(resultado.getObject("fecha_hora_inicio", OffsetDateTime.class));
        cita.setFechaHoraFin(resultado.getObject("fecha_hora_fin", OffsetDateTime.class));
        cita.setMotivoConsulta(resultado.getString("motivo_consulta"));
        cita.setObservaciones(resultado.getString("observaciones"));
        cita.setCanalReserva(resultado.getString("canal_reserva"));
        cita.setMotivoCancelacion(resultado.getString("motivo_cancelacion"));
        Object creador = resultado.getObject("id_usuario_creador");
        if (creador != null) cita.setIdUsuarioCreador(((Number) creador).longValue());
        Object modificador = resultado.getObject("id_usuario_modificacion");
        if (modificador != null) cita.setIdUsuarioModificacion(((Number) modificador).longValue());
        cita.setFechaCreacion(resultado.getObject("fecha_creacion", OffsetDateTime.class));
        cita.setFechaActualizacion(resultado.getObject("fecha_actualizacion", OffsetDateTime.class));
        return cita;
    }
}
