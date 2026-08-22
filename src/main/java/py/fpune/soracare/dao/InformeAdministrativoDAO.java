package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.FiltroInformeDTO;
import py.fpune.soracare.dto.InformeAdministrativoDTO;
import py.fpune.soracare.dto.InformeAdministrativoDTO.CeldaDemandaDTO;
import py.fpune.soracare.dto.InformeAdministrativoDTO.DistribucionDTO;
import py.fpune.soracare.dto.InformeAdministrativoDTO.PuntoFechaDTO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class InformeAdministrativoDAO {

    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Asuncion");

    private static final String FILTRO_CITA = """
             c.fecha_hora_inicio >= ?
         AND c.fecha_hora_inicio < ?
         AND (CAST(? AS BIGINT) IS NULL OR c.id_medico = ?)
         AND (CAST(? AS INTEGER) IS NULL OR c.id_especialidad = ?)
        """;

    public InformeAdministrativoDTO consultar(FiltroInformeDTO filtro) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            ResumenEstados resumen = consultarResumen(conexion, filtro);
            long periodoAnterior = contarCitas(conexion, filtro.periodoAnterior());
            return new InformeAdministrativoDTO(
                    resumen.total(),
                    periodoAnterior,
                    resumen.confirmadas(),
                    resumen.atendidas(),
                    resumen.canceladas(),
                    contarReprogramaciones(conexion, filtro),
                    contarBusquedas(conexion, filtro),
                    contarPacientesRegistrados(conexion, filtro),
                    sumarIngresosConfirmados(conexion, filtro),
                    listarCitasPorFecha(conexion, filtro),
                    listarCitasPorEspecialidad(conexion, filtro),
                    listarCitasPorMedico(conexion, filtro),
                    listarCitasPorEstado(conexion, filtro),
                    listarMapaCalor(conexion, filtro)
            );
        }
    }

    public void registrarBusqueda(
            Long idMedico,
            Integer idEspecialidad,
            LocalDate fechaConsultada,
            int cantidadTurnos,
            String origen
    ) throws SQLException {
        String sql = """
                INSERT INTO soracare.busquedas_disponibilidad (
                    id_medico, id_especialidad, fecha_consultada,
                    cantidad_turnos, origen
                ) VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setObject(1, idMedico);
            sentencia.setObject(2, idEspecialidad);
            sentencia.setObject(3, fechaConsultada);
            sentencia.setInt(4, cantidadTurnos);
            sentencia.setString(5, origen);
            sentencia.executeUpdate();
        }
    }

    private ResumenEstados consultarResumen(Connection conexion, FiltroInformeDTO filtro)
            throws SQLException {
        String sql = """
                SELECT COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE estado.codigo = 'CONFIRMADA') AS confirmadas,
                       COUNT(*) FILTER (WHERE estado.codigo = 'ATENDIDA') AS atendidas,
                       COUNT(*) FILTER (WHERE estado.codigo = 'CANCELADA') AS canceladas
                  FROM soracare.citas c
                  INNER JOIN soracare.estados_citas estado
                          ON estado.id_estado_cita = c.id_estado_cita
                 WHERE
                """ + FILTRO_CITA;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            aplicarFiltroCita(sentencia, filtro, 1);
            try (ResultSet resultado = sentencia.executeQuery()) {
                resultado.next();
                return new ResumenEstados(
                        resultado.getLong("total"),
                        resultado.getLong("confirmadas"),
                        resultado.getLong("atendidas"),
                        resultado.getLong("canceladas")
                );
            }
        }
    }

    private long contarCitas(Connection conexion, FiltroInformeDTO filtro) throws SQLException {
        String sql = "SELECT COUNT(*) FROM soracare.citas c WHERE " + FILTRO_CITA;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            aplicarFiltroCita(sentencia, filtro, 1);
            try (ResultSet resultado = sentencia.executeQuery()) {
                resultado.next();
                return resultado.getLong(1);
            }
        }
    }

    private long contarReprogramaciones(Connection conexion, FiltroInformeDTO filtro)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                  FROM soracare.reprogramaciones_citas r
                 WHERE r.fecha_hora_inicio_nueva >= ?
                   AND r.fecha_hora_inicio_nueva < ?
                   AND (CAST(? AS BIGINT) IS NULL OR r.id_medico_nuevo = ?)
                   AND (CAST(? AS INTEGER) IS NULL OR r.id_especialidad_nueva = ?)
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            aplicarFiltroGenerico(sentencia, filtro, 1);
            try (ResultSet resultado = sentencia.executeQuery()) {
                resultado.next();
                return resultado.getLong(1);
            }
        }
    }

    private long contarBusquedas(Connection conexion, FiltroInformeDTO filtro) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                  FROM soracare.busquedas_disponibilidad busqueda
                 WHERE busqueda.fecha_consultada >= ?
                   AND busqueda.fecha_consultada <= ?
                   AND (CAST(? AS BIGINT) IS NULL OR busqueda.id_medico = ?)
                   AND (CAST(? AS INTEGER) IS NULL OR busqueda.id_especialidad = ?)
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setObject(1, filtro.fechaDesde());
            sentencia.setObject(2, filtro.fechaHasta());
            sentencia.setObject(3, filtro.idMedico());
            sentencia.setObject(4, filtro.idMedico());
            sentencia.setObject(5, filtro.idEspecialidad());
            sentencia.setObject(6, filtro.idEspecialidad());
            try (ResultSet resultado = sentencia.executeQuery()) {
                resultado.next();
                return resultado.getLong(1);
            }
        }
    }

    private long contarPacientesRegistrados(Connection conexion, FiltroInformeDTO filtro)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                  FROM soracare.pacientes
                 WHERE creado_en >= ? AND creado_en < ?
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setObject(1, inicio(filtro.fechaDesde()));
            sentencia.setObject(2, inicio(filtro.fechaHasta().plusDays(1)));
            try (ResultSet resultado = sentencia.executeQuery()) {
                resultado.next();
                return resultado.getLong(1);
            }
        }
    }

    private BigDecimal sumarIngresosConfirmados(Connection conexion, FiltroInformeDTO filtro)
            throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(pago.monto), 0) AS total
                  FROM soracare.pagos pago
                  INNER JOIN soracare.estados_pago estado_pago
                          ON estado_pago.id_estado_pago = pago.id_estado_pago
                  INNER JOIN soracare.citas c ON c.id_cita = pago.id_cita
                 WHERE estado_pago.codigo = 'CONFIRMADO'
                   AND
                """ + FILTRO_CITA;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            aplicarFiltroCita(sentencia, filtro, 1);
            try (ResultSet resultado = sentencia.executeQuery()) {
                resultado.next();
                return resultado.getBigDecimal("total");
            }
        }
    }

    private List<PuntoFechaDTO> listarCitasPorFecha(
            Connection conexion, FiltroInformeDTO filtro
    ) throws SQLException {
        String sql = """
                SELECT (c.fecha_hora_inicio AT TIME ZONE 'America/Asuncion')::DATE AS fecha,
                       COUNT(*) AS total
                  FROM soracare.citas c
                 WHERE
                """ + FILTRO_CITA + """
                 GROUP BY fecha
                 ORDER BY fecha
                """;
        List<PuntoFechaDTO> datos = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            aplicarFiltroCita(sentencia, filtro, 1);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    datos.add(new PuntoFechaDTO(
                            resultado.getObject("fecha", LocalDate.class),
                            resultado.getLong("total")
                    ));
                }
            }
        }
        return datos;
    }

    private List<DistribucionDTO> listarCitasPorEspecialidad(
            Connection conexion, FiltroInformeDTO filtro
    ) throws SQLException {
        String sql = """
                SELECT especialidad.nombre AS etiqueta, COUNT(*) AS total
                  FROM soracare.citas c
                  INNER JOIN soracare.especialidades especialidad
                          ON especialidad.id_especialidad = c.id_especialidad
                 WHERE
                """ + FILTRO_CITA + """
                 GROUP BY especialidad.id_especialidad, especialidad.nombre
                 ORDER BY total DESC, especialidad.nombre
                 LIMIT 8
                """;
        return consultarDistribucion(conexion, sql, filtro);
    }

    private List<DistribucionDTO> listarCitasPorMedico(
            Connection conexion, FiltroInformeDTO filtro
    ) throws SQLException {
        String sql = """
                SELECT CONCAT_WS(' ', persona.nombres, persona.apellidos) AS etiqueta,
                       COUNT(*) AS total
                  FROM soracare.citas c
                  INNER JOIN soracare.medicos medico ON medico.id_medico = c.id_medico
                  INNER JOIN soracare.personas persona ON persona.id_persona = medico.id_persona
                 WHERE
                """ + FILTRO_CITA + """
                 GROUP BY medico.id_medico, persona.nombres, persona.apellidos
                 ORDER BY total DESC, etiqueta
                 LIMIT 8
                """;
        return consultarDistribucion(conexion, sql, filtro);
    }

    private List<DistribucionDTO> listarCitasPorEstado(
            Connection conexion, FiltroInformeDTO filtro
    ) throws SQLException {
        String sql = """
                SELECT estado.nombre AS etiqueta, COUNT(*) AS total
                  FROM soracare.citas c
                  INNER JOIN soracare.estados_citas estado
                          ON estado.id_estado_cita = c.id_estado_cita
                 WHERE
                """ + FILTRO_CITA + """
                 GROUP BY estado.id_estado_cita, estado.nombre
                 ORDER BY total DESC, estado.nombre
                """;
        return consultarDistribucion(conexion, sql, filtro);
    }

    private List<DistribucionDTO> consultarDistribucion(
            Connection conexion, String sql, FiltroInformeDTO filtro
    ) throws SQLException {
        List<DistribucionDTO> datos = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            aplicarFiltroCita(sentencia, filtro, 1);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    datos.add(new DistribucionDTO(
                            resultado.getString("etiqueta"),
                            resultado.getLong("total")
                    ));
                }
            }
        }
        return datos;
    }

    private List<CeldaDemandaDTO> listarMapaCalor(
            Connection conexion, FiltroInformeDTO filtro
    ) throws SQLException {
        String sql = """
                SELECT EXTRACT(ISODOW FROM c.fecha_hora_inicio
                               AT TIME ZONE 'America/Asuncion')::INT AS dia_semana,
                       EXTRACT(HOUR FROM c.fecha_hora_inicio
                              AT TIME ZONE 'America/Asuncion')::INT AS hora,
                       COUNT(*) AS total
                  FROM soracare.citas c
                 WHERE
                """ + FILTRO_CITA + """
                 GROUP BY dia_semana, hora
                 ORDER BY dia_semana, hora
                """;
        List<CeldaDemandaDTO> datos = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            aplicarFiltroCita(sentencia, filtro, 1);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    datos.add(new CeldaDemandaDTO(
                            resultado.getInt("dia_semana"),
                            resultado.getInt("hora"),
                            resultado.getLong("total")
                    ));
                }
            }
        }
        return datos;
    }

    private static void aplicarFiltroCita(
            PreparedStatement sentencia, FiltroInformeDTO filtro, int indice
    ) throws SQLException {
        aplicarFiltroGenerico(sentencia, filtro, indice);
    }

    private static void aplicarFiltroGenerico(
            PreparedStatement sentencia, FiltroInformeDTO filtro, int indice
    ) throws SQLException {
        sentencia.setObject(indice, inicio(filtro.fechaDesde()));
        sentencia.setObject(indice + 1, inicio(filtro.fechaHasta().plusDays(1)));
        sentencia.setObject(indice + 2, filtro.idMedico());
        sentencia.setObject(indice + 3, filtro.idMedico());
        sentencia.setObject(indice + 4, filtro.idEspecialidad());
        sentencia.setObject(indice + 5, filtro.idEspecialidad());
    }

    private static OffsetDateTime inicio(LocalDate fecha) {
        return fecha.atStartOfDay(ZONA_HORARIA).toOffsetDateTime();
    }

    private record ResumenEstados(
            long total, long confirmadas, long atendidas, long canceladas
    ) {
    }
}
