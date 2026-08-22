package py.fpune.soracare.dao;

import py.fpune.soracare.dto.DetalleRecetaDTO;
import py.fpune.soracare.dto.RecetaMedicaDTO;
import py.fpune.soracare.mvc.model.ViaAdministracion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecetaMedicaDAO {

    private static final String SELECT_BASE = """
            SELECT rec.id_receta, rec.id_atencion, rec.id_paciente, rec.id_medico,
                   rec.codigo, rec.fecha_emision, rec.fecha_vencimiento,
                   rec.indicaciones_generales, rec.estado,
                   rec.fecha_anulacion, rec.motivo_anulacion,
                   ate.email_indicaciones_destino,
                   ate.estado_envio_indicaciones_email,
                   ate.fecha_envio_indicaciones_email,
                   CONCAT_WS(' ', pper.nombres, pper.apellidos) AS nombre_paciente,
                   CONCAT(
                       CASE med.tratamiento WHEN 'DRA' THEN 'Dra.' ELSE 'Dr.' END,
                       ' ', mper.nombres, ' ', mper.apellidos
                   ) AS nombre_medico,
                   esp.nombre AS especialidad
              FROM soracare.recetas_medicas rec
              INNER JOIN soracare.pacientes pac ON pac.id_paciente = rec.id_paciente
              INNER JOIN soracare.personas pper ON pper.id_persona = pac.id_persona
              INNER JOIN soracare.medicos med ON med.id_medico = rec.id_medico
              INNER JOIN soracare.personas mper ON mper.id_persona = med.id_persona
              INNER JOIN soracare.atenciones_medicas ate ON ate.id_atencion = rec.id_atencion
              INNER JOIN soracare.citas cit ON cit.id_cita = ate.id_cita
              INNER JOIN soracare.especialidades esp ON esp.id_especialidad = cit.id_especialidad
            """;

    public RecetaMedicaDTO crear(
            Connection conexion,
            long idAtencion,
            long idPaciente,
            long idMedico,
            LocalDate fechaVencimiento,
            String indicacionesGenerales,
            List<DetalleNuevo> detalles
    ) throws SQLException {
        String sql = """
                INSERT INTO soracare.recetas_medicas (
                    id_atencion, id_paciente, id_medico,
                    fecha_vencimiento, indicaciones_generales
                ) VALUES (?, ?, ?, ?, ?)
                RETURNING id_receta
                """;
        long idReceta;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idAtencion);
            sentencia.setLong(2, idPaciente);
            sentencia.setLong(3, idMedico);
            if (fechaVencimiento == null) sentencia.setNull(4, Types.DATE);
            else sentencia.setObject(4, fechaVencimiento);
            if (indicacionesGenerales == null) sentencia.setNull(5, Types.VARCHAR);
            else sentencia.setString(5, indicacionesGenerales);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException("No fue posible crear la receta médica.");
                }
                idReceta = resultado.getLong("id_receta");
            }
        }
        insertarDetalles(conexion, idReceta, detalles);
        return buscarPorId(conexion, idReceta)
                .orElseThrow(() -> new SQLException("No fue posible recuperar la receta emitida."));
    }

    public List<RecetaMedicaDTO> listarPorAtencion(long idAtencion) throws SQLException {
        try (Connection conexion = py.fpune.soracare.config.ConexionBD.obtenerConexion()) {
            return listar(conexion, SELECT_BASE
                    + " WHERE rec.id_atencion = ? ORDER BY rec.fecha_emision DESC", idAtencion);
        }
    }

    public List<RecetaMedicaDTO> listarPorPaciente(long idPaciente) throws SQLException {
        try (Connection conexion = py.fpune.soracare.config.ConexionBD.obtenerConexion()) {
            return listar(conexion, SELECT_BASE
                    + " WHERE rec.id_paciente = ? ORDER BY rec.fecha_emision DESC", idPaciente);
        }
    }

    public Optional<RecetaMedicaDTO> buscarPorId(Connection conexion, long idReceta)
            throws SQLException {
        List<RecetaMedicaDTO> resultados = listar(
                conexion, SELECT_BASE + " WHERE rec.id_receta = ?", idReceta);
        return resultados.stream().findFirst();
    }

    public void anular(
            Connection conexion,
            long idReceta,
            long idMedico,
            long idUsuario,
            String motivo
    ) throws SQLException {
        String sql = """
                UPDATE soracare.recetas_medicas
                   SET estado = 'ANULADA', fecha_anulacion = CURRENT_TIMESTAMP,
                       motivo_anulacion = ?, id_usuario_anulacion = ?
                 WHERE id_receta = ? AND id_medico = ? AND estado = 'EMITIDA'
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, motivo);
            sentencia.setLong(2, idUsuario);
            sentencia.setLong(3, idReceta);
            sentencia.setLong(4, idMedico);
            if (sentencia.executeUpdate() != 1) {
                throw new IllegalArgumentException(
                        "La receta no existe, no pertenece al médico o ya no puede anularse.");
            }
        }
    }

    private static void insertarDetalles(
            Connection conexion, long idReceta, List<DetalleNuevo> detalles
    ) throws SQLException {
        String sql = """
                INSERT INTO soracare.recetas_detalles (
                    id_receta, id_medicamento, dosis, frecuencia,
                    duracion_dias, cantidad, unidad, instrucciones
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (DetalleNuevo detalle : detalles) {
                sentencia.setLong(1, idReceta);
                sentencia.setLong(2, detalle.idMedicamento());
                sentencia.setString(3, detalle.dosis());
                sentencia.setString(4, detalle.frecuencia());
                if (detalle.duracionDias() == null) sentencia.setNull(5, Types.SMALLINT);
                else sentencia.setInt(5, detalle.duracionDias());
                sentencia.setBigDecimal(6, detalle.cantidad());
                sentencia.setString(7, detalle.unidad());
                if (detalle.instrucciones() == null) sentencia.setNull(8, Types.VARCHAR);
                else sentencia.setString(8, detalle.instrucciones());
                sentencia.addBatch();
            }
            int[] resultados = sentencia.executeBatch();
            if (resultados.length != detalles.size()) {
                throw new SQLException("No fue posible registrar todos los medicamentos.");
            }
        }
    }

    private static List<RecetaMedicaDTO> listar(
            Connection conexion, String sql, long id
    ) throws SQLException {
        List<RecetaMedicaDTO> recetas = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    long idReceta = resultado.getLong("id_receta");
                    recetas.add(new RecetaMedicaDTO(
                            idReceta,
                            resultado.getLong("id_atencion"),
                            resultado.getLong("id_paciente"),
                            resultado.getLong("id_medico"),
                            resultado.getString("codigo"),
                            resultado.getObject("fecha_emision", OffsetDateTime.class),
                            resultado.getObject("fecha_vencimiento", LocalDate.class),
                            resultado.getString("indicaciones_generales"),
                            resultado.getString("estado"),
                            resultado.getObject("fecha_anulacion", OffsetDateTime.class),
                            resultado.getString("motivo_anulacion"),
                            resultado.getString("nombre_paciente"),
                            resultado.getString("nombre_medico"),
                            resultado.getString("especialidad"),
                            resultado.getString("email_indicaciones_destino"),
                            resultado.getString("estado_envio_indicaciones_email"),
                            resultado.getObject(
                                    "fecha_envio_indicaciones_email",
                                    OffsetDateTime.class),
                            listarDetalles(conexion, idReceta)
                    ));
                }
            }
        }
        return recetas;
    }

    private static List<DetalleRecetaDTO> listarDetalles(
            Connection conexion, long idReceta
    ) throws SQLException {
        String sql = """
                SELECT det.id_detalle_receta, det.id_medicamento,
                       med.nombre_comercial, med.principio_activo,
                       med.presentacion, med.concentracion, med.via_administracion,
                       det.dosis, det.frecuencia, det.duracion_dias,
                       det.cantidad, det.unidad, det.instrucciones
                  FROM soracare.recetas_detalles det
                  INNER JOIN soracare.medicamentos med
                          ON med.id_medicamento = det.id_medicamento
                 WHERE det.id_receta = ?
                 ORDER BY det.id_detalle_receta
                """;
        List<DetalleRecetaDTO> detalles = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idReceta);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    int duracion = resultado.getInt("duracion_dias");
                    Integer duracionDias = resultado.wasNull() ? null : duracion;
                    detalles.add(new DetalleRecetaDTO(
                            resultado.getLong("id_detalle_receta"),
                            resultado.getLong("id_medicamento"),
                            resultado.getString("nombre_comercial"),
                            resultado.getString("principio_activo"),
                            resultado.getString("presentacion"),
                            resultado.getString("concentracion"),
                            ViaAdministracion.valueOf(resultado.getString("via_administracion")),
                            resultado.getString("dosis"),
                            resultado.getString("frecuencia"),
                            duracionDias,
                            resultado.getBigDecimal("cantidad"),
                            resultado.getString("unidad"),
                            resultado.getString("instrucciones")
                    ));
                }
            }
        }
        return detalles;
    }

    public record DetalleNuevo(
            long idMedicamento,
            String dosis,
            String frecuencia,
            Integer duracionDias,
            BigDecimal cantidad,
            String unidad,
            String instrucciones
    ) {
    }
}
