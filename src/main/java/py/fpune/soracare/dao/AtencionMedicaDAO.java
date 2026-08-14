package py.fpune.soracare.dao;

import py.fpune.soracare.dto.AtencionMedicaDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AtencionMedicaDAO {

    private static final String COLUMNAS = """
            ate.id_atencion,
            ate.id_cita,
            ate.id_paciente,
            ate.id_medico,
            ate.fecha_hora_inicio,
            ate.fecha_hora_fin,
            ate.motivo_consulta,
            ate.sintomas,
            ate.antecedentes_relevantes,
            ate.temperatura_c,
            ate.frecuencia_cardiaca,
            ate.frecuencia_respiratoria,
            ate.presion_sistolica,
            ate.presion_diastolica,
            ate.saturacion_oxigeno,
            ate.evaluacion_area_afectada,
            ate.examen_fisico,
            ate.evaluacion_clinica,
            ate.plan_tratamiento,
            ate.indicaciones,
            ate.observaciones,
            ate.requiere_seguimiento,
            ate.fecha_proximo_control,
            ate.estado,
            CONCAT_WS(' ', per.nombres, per.apellidos) AS medico_nombre_completo,
            esp.nombre AS especialidad
            """;

    private static final String BASE_SELECT = "SELECT " + COLUMNAS + """
            FROM soracare.atenciones_medicas ate
            INNER JOIN soracare.medicos med
                ON med.id_medico = ate.id_medico
            INNER JOIN soracare.personas per
                ON per.id_persona = med.id_persona
            INNER JOIN soracare.citas cit
                ON cit.id_cita = ate.id_cita
            INNER JOIN soracare.especialidades esp
                ON esp.id_especialidad = cit.id_especialidad
            """;

    private static final String SQL_INSERTAR = """
            INSERT INTO soracare.atenciones_medicas (
                id_cita,
                id_paciente,
                id_medico,
                motivo_consulta
            ) VALUES (?, ?, ?, ?)
            RETURNING id_atencion
            """;

    private static final String SQL_ACTUALIZAR = """
            UPDATE soracare.atenciones_medicas
            SET sintomas = ?,
                antecedentes_relevantes = ?,
                temperatura_c = ?,
                frecuencia_cardiaca = ?,
                frecuencia_respiratoria = ?,
                presion_sistolica = ?,
                presion_diastolica = ?,
                saturacion_oxigeno = ?,
                evaluacion_area_afectada = ?,
                examen_fisico = ?,
                evaluacion_clinica = ?,
                plan_tratamiento = ?,
                indicaciones = ?,
                observaciones = ?,
                requiere_seguimiento = ?,
                fecha_proximo_control = ?,
                estado = ?,
                fecha_hora_fin = CASE
                    WHEN ? = 'FINALIZADA' THEN CURRENT_TIMESTAMP
                    ELSE fecha_hora_fin
                END
            WHERE id_atencion = ?
              AND id_medico = ?
              AND estado = 'EN_CURSO'
            """;

    public AtencionMedicaDTO iniciar(
            Connection conexion,
            long idCita,
            long idPaciente,
            long idMedico,
            String motivoConsulta
    ) throws SQLException {
        long idAtencion;
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR)) {
            sentencia.setLong(1, idCita);
            sentencia.setLong(2, idPaciente);
            sentencia.setLong(3, idMedico);
            sentencia.setString(4, motivoConsulta);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException("No fue posible iniciar la atención médica.");
                }
                idAtencion = resultado.getLong("id_atencion");
            }
        }
        return buscarPorId(conexion, idAtencion)
                .orElseThrow(() -> new SQLException(
                        "No fue posible recuperar la atención iniciada."));
    }

    public Optional<AtencionMedicaDTO> buscarPorCita(
            Connection conexion,
            long idCita
    ) throws SQLException {
        return buscarUno(conexion, BASE_SELECT + " WHERE ate.id_cita = ?", idCita);
    }

    public Optional<AtencionMedicaDTO> buscarPorCita(long idCita)
            throws SQLException {
        try (Connection conexion = py.fpune.soracare.config.ConexionBD.obtenerConexion()) {
            return buscarPorCita(conexion, idCita);
        }
    }

    public List<AtencionMedicaDTO> listarHistorialPaciente(
            long idPaciente,
            long idCitaActual,
            int limite
    ) throws SQLException {
        String sql = BASE_SELECT + """
                WHERE ate.id_paciente = ?
                  AND ate.id_cita <> ?
                  AND ate.estado = 'FINALIZADA'
                ORDER BY ate.fecha_hora_inicio DESC, ate.id_atencion DESC
                LIMIT ?
                """;
        List<AtencionMedicaDTO> atenciones = new ArrayList<>();
        try (Connection conexion = py.fpune.soracare.config.ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPaciente);
            sentencia.setLong(2, idCitaActual);
            sentencia.setInt(3, limite);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    atenciones.add(mapear(resultado));
                }
            }
        }
        return atenciones;
    }

    public AtencionMedicaDTO actualizar(
            Connection conexion,
            long idAtencion,
            long idMedico,
            String sintomas,
            String antecedentesRelevantes,
            BigDecimal temperaturaC,
            Integer frecuenciaCardiaca,
            Integer frecuenciaRespiratoria,
            Integer presionSistolica,
            Integer presionDiastolica,
            Integer saturacionOxigeno,
            String evaluacionAreaAfectada,
            String examenFisico,
            String evaluacionClinica,
            String planTratamiento,
            String indicaciones,
            String observaciones,
            boolean requiereSeguimiento,
            LocalDate fechaProximoControl,
            String estado
    ) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTUALIZAR)) {
            establecerTexto(sentencia, 1, sintomas);
            establecerTexto(sentencia, 2, antecedentesRelevantes);
            if (temperaturaC == null) {
                sentencia.setNull(3, Types.NUMERIC);
            } else {
                sentencia.setBigDecimal(3, temperaturaC);
            }
            establecerEntero(sentencia, 4, frecuenciaCardiaca);
            establecerEntero(sentencia, 5, frecuenciaRespiratoria);
            establecerEntero(sentencia, 6, presionSistolica);
            establecerEntero(sentencia, 7, presionDiastolica);
            establecerEntero(sentencia, 8, saturacionOxigeno);
            establecerTexto(sentencia, 9, evaluacionAreaAfectada);
            establecerTexto(sentencia, 10, examenFisico);
            establecerTexto(sentencia, 11, evaluacionClinica);
            establecerTexto(sentencia, 12, planTratamiento);
            establecerTexto(sentencia, 13, indicaciones);
            establecerTexto(sentencia, 14, observaciones);
            sentencia.setBoolean(15, requiereSeguimiento);
            if (fechaProximoControl == null) {
                sentencia.setNull(16, Types.DATE);
            } else {
                sentencia.setObject(16, fechaProximoControl);
            }
            sentencia.setString(17, estado);
            sentencia.setString(18, estado);
            sentencia.setLong(19, idAtencion);
            sentencia.setLong(20, idMedico);
            if (sentencia.executeUpdate() != 1) {
                throw new IllegalArgumentException(
                        "La atención ya fue finalizada o no pertenece al médico autenticado.");
            }
        }
        return buscarPorId(conexion, idAtencion)
                .orElseThrow(() -> new SQLException(
                        "No fue posible recuperar la atención actualizada."));
    }

    private Optional<AtencionMedicaDTO> buscarPorId(
            Connection conexion,
            long idAtencion
    ) throws SQLException {
        return buscarUno(
                conexion,
                BASE_SELECT + " WHERE ate.id_atencion = ?",
                idAtencion
        );
    }

    private Optional<AtencionMedicaDTO> buscarUno(
            Connection conexion,
            String sql,
            long id
    ) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next()
                        ? Optional.of(mapear(resultado))
                        : Optional.empty();
            }
        }
    }

    private static AtencionMedicaDTO mapear(ResultSet resultado)
            throws SQLException {
        return new AtencionMedicaDTO(
                resultado.getLong("id_atencion"),
                resultado.getLong("id_cita"),
                resultado.getLong("id_paciente"),
                resultado.getLong("id_medico"),
                resultado.getObject("fecha_hora_inicio", OffsetDateTime.class),
                resultado.getObject("fecha_hora_fin", OffsetDateTime.class),
                resultado.getString("motivo_consulta"),
                resultado.getString("sintomas"),
                resultado.getString("antecedentes_relevantes"),
                resultado.getBigDecimal("temperatura_c"),
                enteroNulo(resultado, "frecuencia_cardiaca"),
                enteroNulo(resultado, "frecuencia_respiratoria"),
                enteroNulo(resultado, "presion_sistolica"),
                enteroNulo(resultado, "presion_diastolica"),
                enteroNulo(resultado, "saturacion_oxigeno"),
                resultado.getString("evaluacion_area_afectada"),
                resultado.getString("examen_fisico"),
                resultado.getString("evaluacion_clinica"),
                resultado.getString("plan_tratamiento"),
                resultado.getString("indicaciones"),
                resultado.getString("observaciones"),
                resultado.getBoolean("requiere_seguimiento"),
                resultado.getObject("fecha_proximo_control", LocalDate.class),
                resultado.getString("estado"),
                resultado.getString("medico_nombre_completo"),
                resultado.getString("especialidad")
        );
    }

    private static void establecerTexto(
            PreparedStatement sentencia,
            int indice,
            String valor
    ) throws SQLException {
        if (valor == null) {
            sentencia.setNull(indice, Types.VARCHAR);
        } else {
            sentencia.setString(indice, valor);
        }
    }

    private static void establecerEntero(
            PreparedStatement sentencia,
            int indice,
            Integer valor
    ) throws SQLException {
        if (valor == null) {
            sentencia.setNull(indice, Types.SMALLINT);
        } else {
            sentencia.setInt(indice, valor);
        }
    }

    private static Integer enteroNulo(ResultSet resultado, String columna)
            throws SQLException {
        int valor = resultado.getInt(columna);
        return resultado.wasNull() ? null : valor;
    }
}
