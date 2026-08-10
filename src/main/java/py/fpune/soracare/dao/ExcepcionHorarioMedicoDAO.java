package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.mvc.model.ExcepcionHorarioMedico;
import py.fpune.soracare.mvc.model.TipoExcepcionHorario;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExcepcionHorarioMedicoDAO {

    private static final String COLUMNAS = """
            id_excepcion, id_medico, id_especialidad, id_consultorio,
            fecha, hora_inicio, hora_fin, tipo, motivo, activo,
            fecha_creacion, fecha_actualizacion
            """;

    private static final String SQL_INSERTAR = """
            INSERT INTO soracare.excepciones_horarios_medicos (
                id_medico, id_especialidad, id_consultorio, fecha,
                hora_inicio, hora_fin, tipo, motivo, activo
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)
            RETURNING
            """ + COLUMNAS;

    private static final String SQL_LISTAR_TODAS = """
            SELECT
            """ + COLUMNAS + """
            FROM soracare.excepciones_horarios_medicos
            ORDER BY fecha DESC, hora_inicio ASC NULLS FIRST, id_excepcion DESC
            """;

    private static final String SQL_LISTAR_ACTIVAS_FECHA = """
            SELECT
            """ + COLUMNAS + """
            FROM soracare.excepciones_horarios_medicos
            WHERE activo = TRUE AND fecha = ?
            ORDER BY hora_inicio ASC NULLS FIRST, id_excepcion ASC
            """;

    private static final String SQL_BUSCAR_POR_ID = """
            SELECT
            """ + COLUMNAS + """
            FROM soracare.excepciones_horarios_medicos
            WHERE id_excepcion = ?
            """;

    private static final String SQL_ACTUALIZAR_ACTIVO = """
            UPDATE soracare.excepciones_horarios_medicos
            SET activo = ?, fecha_actualizacion = CURRENT_TIMESTAMP
            WHERE id_excepcion = ?
            """;

    public ExcepcionHorarioMedico crear(
            Connection conexion,
            ExcepcionHorarioMedico excepcion
    ) throws SQLException {
        validarConexion(conexion);

        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR)) {
            sentencia.setLong(1, excepcion.getIdMedico());
            establecerEnteroOpcional(sentencia, 2, excepcion.getIdEspecialidad());
            establecerLongOpcional(sentencia, 3, excepcion.getIdConsultorio());
            sentencia.setDate(4, Date.valueOf(excepcion.getFecha()));
            establecerHoraOpcional(sentencia, 5, excepcion.getHoraInicio());
            establecerHoraOpcional(sentencia, 6, excepcion.getHoraFin());
            sentencia.setString(7, excepcion.getTipo().name());
            sentencia.setString(8, excepcion.getMotivo());

            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException("PostgreSQL no devolvió la excepción creada.");
                }
                return mapear(resultado);
            }
        }
    }

    public List<ExcepcionHorarioMedico> listarTodas() throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            return listar(conexion, SQL_LISTAR_TODAS, null);
        }
    }

    public List<ExcepcionHorarioMedico> listarActivasEnFecha(LocalDate fecha)
            throws SQLException {
        if (fecha == null) {
            throw new SQLException("La fecha de excepciones es obligatoria.");
        }
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            return listar(conexion, SQL_LISTAR_ACTIVAS_FECHA, fecha);
        }
    }

    public Optional<ExcepcionHorarioMedico> buscarPorId(
            Connection conexion,
            long idExcepcion
    ) throws SQLException {
        validarConexion(conexion);
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR_POR_ID)) {
            sentencia.setLong(1, idExcepcion);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        }
    }

    public void actualizarActivo(Connection conexion, long idExcepcion, boolean activo)
            throws SQLException {
        validarConexion(conexion);
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTUALIZAR_ACTIVO)) {
            sentencia.setBoolean(1, activo);
            sentencia.setLong(2, idExcepcion);
            if (sentencia.executeUpdate() != 1) {
                throw new SQLException("No fue posible actualizar la excepción de agenda.");
            }
        }
    }

    private static List<ExcepcionHorarioMedico> listar(
            Connection conexion,
            String sql,
            LocalDate fecha
    ) throws SQLException {
        List<ExcepcionHorarioMedico> excepciones = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            if (fecha != null) {
                sentencia.setDate(1, Date.valueOf(fecha));
            }
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    excepciones.add(mapear(resultado));
                }
            }
        }
        return excepciones;
    }

    private static ExcepcionHorarioMedico mapear(ResultSet resultado) throws SQLException {
        ExcepcionHorarioMedico excepcion = new ExcepcionHorarioMedico();
        excepcion.setIdExcepcion(resultado.getLong("id_excepcion"));
        excepcion.setIdMedico(resultado.getLong("id_medico"));
        Object idEspecialidad = resultado.getObject("id_especialidad");
        if (idEspecialidad != null) excepcion.setIdEspecialidad(((Number) idEspecialidad).intValue());
        Object idConsultorio = resultado.getObject("id_consultorio");
        if (idConsultorio != null) excepcion.setIdConsultorio(((Number) idConsultorio).longValue());
        excepcion.setFecha(resultado.getDate("fecha").toLocalDate());
        Time inicio = resultado.getTime("hora_inicio");
        Time fin = resultado.getTime("hora_fin");
        if (inicio != null) excepcion.setHoraInicio(inicio.toLocalTime());
        if (fin != null) excepcion.setHoraFin(fin.toLocalTime());
        excepcion.setTipo(TipoExcepcionHorario.valueOf(resultado.getString("tipo")));
        excepcion.setMotivo(resultado.getString("motivo"));
        excepcion.setActivo(resultado.getBoolean("activo"));
        excepcion.setFechaCreacion(resultado.getObject("fecha_creacion", OffsetDateTime.class));
        excepcion.setFechaActualizacion(resultado.getObject("fecha_actualizacion", OffsetDateTime.class));
        return excepcion;
    }

    private static void establecerEnteroOpcional(PreparedStatement s, int p, Integer v)
            throws SQLException {
        if (v == null) s.setNull(p, Types.SMALLINT); else s.setInt(p, v);
    }

    private static void establecerLongOpcional(PreparedStatement s, int p, Long v)
            throws SQLException {
        if (v == null) s.setNull(p, Types.BIGINT); else s.setLong(p, v);
    }

    private static void establecerHoraOpcional(PreparedStatement s, int p, java.time.LocalTime v)
            throws SQLException {
        if (v == null) s.setNull(p, Types.TIME); else s.setTime(p, Time.valueOf(v));
    }

    private static void validarConexion(Connection conexion) throws SQLException {
        if (conexion == null) throw new SQLException("La conexión no puede ser nula.");
    }
}
