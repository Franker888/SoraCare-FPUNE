package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.mvc.model.HorarioMedico;

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

public class HorarioMedicoDAO {

    private static final String SQL_INSERTAR = """
            INSERT INTO soracare.horarios_medicos (
                id_medico,
                id_especialidad,
                id_consultorio,
                dia_semana,
                hora_inicio,
                hora_fin,
                duracion_turno_minutos,
                fecha_desde,
                fecha_hasta,
                activo
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING
                id_horario,
                id_medico,
                id_especialidad,
                id_consultorio,
                dia_semana,
                hora_inicio,
                hora_fin,
                duracion_turno_minutos,
                fecha_desde,
                fecha_hasta,
                activo,
                fecha_creacion,
                fecha_actualizacion
            """;

    private static final String SQL_BUSCAR_POR_ID = """
            SELECT
                id_horario,
                id_medico,
                id_especialidad,
                id_consultorio,
                dia_semana,
                hora_inicio,
                hora_fin,
                duracion_turno_minutos,
                fecha_desde,
                fecha_hasta,
                activo,
                fecha_creacion,
                fecha_actualizacion
            FROM soracare.horarios_medicos
            WHERE id_horario = ?
            """;

    private static final String SQL_LISTAR_TODOS = """
            SELECT
                id_horario,
                id_medico,
                id_especialidad,
                id_consultorio,
                dia_semana,
                hora_inicio,
                hora_fin,
                duracion_turno_minutos,
                fecha_desde,
                fecha_hasta,
                activo,
                fecha_creacion,
                fecha_actualizacion
            FROM soracare.horarios_medicos
            ORDER BY
                activo DESC,
                dia_semana ASC,
                hora_inicio ASC,
                id_medico ASC
            """;

    private static final String SQL_LISTAR_VIGENTES_FECHA = """
            SELECT
                id_horario,
                id_medico,
                id_especialidad,
                id_consultorio,
                dia_semana,
                hora_inicio,
                hora_fin,
                duracion_turno_minutos,
                fecha_desde,
                fecha_hasta,
                activo,
                fecha_creacion,
                fecha_actualizacion
            FROM soracare.horarios_medicos
            WHERE activo = TRUE
              AND fecha_desde <= ?
              AND (
                    fecha_hasta IS NULL
                    OR fecha_hasta >= ?
                  )
            ORDER BY
                dia_semana ASC,
                hora_inicio ASC,
                id_medico ASC
            """;

    private static final String SQL_EXISTE_SUPERPOSICION = """
            SELECT EXISTS (
                SELECT 1
                FROM soracare.horarios_medicos horario
                WHERE horario.activo = TRUE
                  AND horario.id_horario <> ?
                  AND horario.dia_semana = ?
                  AND ? < horario.hora_fin
                  AND ? > horario.hora_inicio
                  AND (
                        horario.fecha_hasta IS NULL
                        OR horario.fecha_hasta >= ?
                      )
                  AND (
                        CAST(? AS DATE) IS NULL
                        OR horario.fecha_desde <= ?
                      )
                  AND (
                        horario.id_medico = ?
                        OR horario.id_consultorio = ?
                      )
            ) AS existe
            """;

    private static final String SQL_ACTUALIZAR_ACTIVO = """
            UPDATE soracare.horarios_medicos
            SET activo = ?,
                fecha_actualizacion = CURRENT_TIMESTAMP
            WHERE id_horario = ?
            """;

    public HorarioMedico crear(
            Connection conexion,
            HorarioMedico horario
    ) throws SQLException {

        validarConexion(
                conexion
        );

        if (horario == null) {
            throw new SQLException(
                    "El horario médico no puede ser nulo."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_INSERTAR
                        )
        ) {
            sentencia.setLong(
                    1,
                    horario.getIdMedico()
            );

            sentencia.setInt(
                    2,
                    horario.getIdEspecialidad()
            );

            sentencia.setLong(
                    3,
                    horario.getIdConsultorio()
            );

            sentencia.setInt(
                    4,
                    horario.getDiaSemana()
            );

            sentencia.setTime(
                    5,
                    Time.valueOf(
                            horario.getHoraInicio()
                    )
            );

            sentencia.setTime(
                    6,
                    Time.valueOf(
                            horario.getHoraFin()
                    )
            );

            sentencia.setInt(
                    7,
                    horario.getDuracionTurnoMinutos()
            );

            sentencia.setDate(
                    8,
                    Date.valueOf(
                            horario.getFechaDesde()
                    )
            );

            establecerFechaOpcional(
                    sentencia,
                    9,
                    horario.getFechaHasta()
            );

            sentencia.setBoolean(
                    10,
                    horario.isActivo()
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "PostgreSQL no devolvió el horario creado."
                    );
                }

                return mapearHorario(
                        resultado
                );
            }
        }
    }

    public Optional<HorarioMedico> buscarPorId(
            long idHorario
    ) throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return buscarPorId(
                    conexion,
                    idHorario
            );
        }
    }

    public Optional<HorarioMedico> buscarPorId(
            Connection conexion,
            long idHorario
    ) throws SQLException {

        validarConexion(
                conexion
        );

        validarIdHorario(
                idHorario
        );

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_BUSCAR_POR_ID
                        )
        ) {
            sentencia.setLong(
                    1,
                    idHorario
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                if (!resultado.next()) {
                    return Optional.empty();
                }

                return Optional.of(
                        mapearHorario(
                                resultado
                        )
                );
            }
        }
    }

    public List<HorarioMedico> listarTodos()
            throws SQLException {

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return ejecutarListado(
                    conexion,
                    SQL_LISTAR_TODOS,
                    null
            );
        }
    }

    public List<HorarioMedico> listarVigentesEnFecha(
            LocalDate fecha
    ) throws SQLException {

        if (fecha == null) {
            throw new SQLException(
                    "La fecha de consulta es obligatoria."
            );
        }

        try (
                Connection conexion =
                        ConexionBD.obtenerConexion()
        ) {
            return ejecutarListado(
                    conexion,
                    SQL_LISTAR_VIGENTES_FECHA,
                    fecha
            );
        }
    }

    public boolean existeSuperposicion(
            Connection conexion,
            HorarioMedico horario,
            long idHorarioExcluido
    ) throws SQLException {

        validarConexion(
                conexion
        );

        if (horario == null) {
            throw new SQLException(
                    "El horario médico no puede ser nulo."
            );
        }

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_EXISTE_SUPERPOSICION
                        )
        ) {
            sentencia.setLong(
                    1,
                    Math.max(
                            idHorarioExcluido,
                            0
                    )
            );

            sentencia.setInt(
                    2,
                    horario.getDiaSemana()
            );

            sentencia.setTime(
                    3,
                    Time.valueOf(
                            horario.getHoraInicio()
                    )
            );

            sentencia.setTime(
                    4,
                    Time.valueOf(
                            horario.getHoraFin()
                    )
            );

            sentencia.setDate(
                    5,
                    Date.valueOf(
                            horario.getFechaDesde()
                    )
            );

            establecerFechaOpcional(
                    sentencia,
                    6,
                    horario.getFechaHasta()
            );

            establecerFechaOpcional(
                    sentencia,
                    7,
                    horario.getFechaHasta()
            );

            sentencia.setLong(
                    8,
                    horario.getIdMedico()
            );

            sentencia.setLong(
                    9,
                    horario.getIdConsultorio()
            );

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                return resultado.next()
                        && resultado.getBoolean(
                        "existe"
                );
            }
        }
    }

    public void actualizarActivo(
            Connection conexion,
            long idHorario,
            boolean activo
    ) throws SQLException {

        validarConexion(
                conexion
        );

        validarIdHorario(
                idHorario
        );

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                SQL_ACTUALIZAR_ACTIVO
                        )
        ) {
            sentencia.setBoolean(
                    1,
                    activo
            );

            sentencia.setLong(
                    2,
                    idHorario
            );

            int filasActualizadas =
                    sentencia.executeUpdate();

            if (filasActualizadas != 1) {
                throw new SQLException(
                        "No fue posible actualizar el horario médico."
                );
            }
        }
    }

    private static List<HorarioMedico> ejecutarListado(
            Connection conexion,
            String sql,
            LocalDate fecha
    ) throws SQLException {

        validarConexion(
                conexion
        );

        List<HorarioMedico> horarios =
                new ArrayList<>();

        try (
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                sql
                        )
        ) {
            if (fecha != null) {
                sentencia.setDate(
                        1,
                        Date.valueOf(
                                fecha
                        )
                );

                sentencia.setDate(
                        2,
                        Date.valueOf(
                                fecha
                        )
                );
            }

            try (
                    ResultSet resultado =
                            sentencia.executeQuery()
            ) {
                while (resultado.next()) {
                    horarios.add(
                            mapearHorario(
                                    resultado
                            )
                    );
                }
            }
        }

        return horarios;
    }

    private static HorarioMedico mapearHorario(
            ResultSet resultado
    ) throws SQLException {

        HorarioMedico horario =
                new HorarioMedico();

        horario.setIdHorario(
                resultado.getLong(
                        "id_horario"
                )
        );

        horario.setIdMedico(
                resultado.getLong(
                        "id_medico"
                )
        );

        horario.setIdEspecialidad(
                resultado.getInt(
                        "id_especialidad"
                )
        );

        horario.setIdConsultorio(
                resultado.getLong(
                        "id_consultorio"
                )
        );

        horario.setDiaSemana(
                resultado.getInt(
                        "dia_semana"
                )
        );

        horario.setHoraInicio(
                resultado.getTime(
                        "hora_inicio"
                ).toLocalTime()
        );

        horario.setHoraFin(
                resultado.getTime(
                        "hora_fin"
                ).toLocalTime()
        );

        horario.setDuracionTurnoMinutos(
                resultado.getInt(
                        "duracion_turno_minutos"
                )
        );

        horario.setFechaDesde(
                resultado.getDate(
                        "fecha_desde"
                ).toLocalDate()
        );

        Date fechaHasta =
                resultado.getDate(
                        "fecha_hasta"
                );

        if (fechaHasta != null) {
            horario.setFechaHasta(
                    fechaHasta.toLocalDate()
            );
        }

        horario.setActivo(
                resultado.getBoolean(
                        "activo"
                )
        );

        horario.setFechaCreacion(
                resultado.getObject(
                        "fecha_creacion",
                        OffsetDateTime.class
                )
        );

        horario.setFechaActualizacion(
                resultado.getObject(
                        "fecha_actualizacion",
                        OffsetDateTime.class
                )
        );

        return horario;
    }

    private static void establecerFechaOpcional(
            PreparedStatement sentencia,
            int posicion,
            LocalDate fecha
    ) throws SQLException {

        if (fecha == null) {
            sentencia.setNull(
                    posicion,
                    Types.DATE
            );
        } else {
            sentencia.setDate(
                    posicion,
                    Date.valueOf(
                            fecha
                    )
            );
        }
    }

    private static void validarIdHorario(
            long idHorario
    ) throws SQLException {

        if (idHorario <= 0) {
            throw new SQLException(
                    "El ID del horario debe ser mayor que cero."
            );
        }
    }

    private static void validarConexion(
            Connection conexion
    ) throws SQLException {

        if (conexion == null) {
            throw new SQLException(
                    "La conexión no puede ser nula."
            );
        }
    }
}
