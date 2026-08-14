package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.ComprobantePagoDTO;
import py.fpune.soracare.dto.DatosFacturacionDTO;
import py.fpune.soracare.dto.FiltroAgendaPagosDTO;
import py.fpune.soracare.dto.PagoResumenDTO;
import py.fpune.soracare.dto.ResultadoAgendaPagosDTO;
import py.fpune.soracare.mvc.model.MetodoPago;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PagoDAO {

    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Asuncion");

    private static final String RESUMEN_FROM = """
              FROM soracare.pagos p
              INNER JOIN soracare.metodos_pago mp
                      ON mp.id_metodo_pago = p.id_metodo_pago
              INNER JOIN soracare.estados_pago ep
                      ON ep.id_estado_pago = p.id_estado_pago
              INNER JOIN soracare.citas c ON c.id_cita = p.id_cita
              INNER JOIN soracare.pacientes paciente
                      ON paciente.id_paciente = c.id_paciente
              INNER JOIN soracare.personas paciente_persona
                      ON paciente_persona.id_persona = paciente.id_persona
              INNER JOIN soracare.medicos medico ON medico.id_medico = c.id_medico
              INNER JOIN soracare.personas medico_persona
                      ON medico_persona.id_persona = medico.id_persona
              INNER JOIN soracare.especialidades especialidad
                      ON especialidad.id_especialidad = c.id_especialidad
              LEFT JOIN soracare.datos_facturacion_pago datos
                     ON datos.id_pago = p.id_pago
              LEFT JOIN soracare.comprobantes_pago comprobante
                     ON comprobante.id_pago = p.id_pago
            """;

    private static final String RESUMEN_SELECT = """
            SELECT p.id_pago, p.id_cita, c.id_paciente,
                   mp.codigo AS codigo_metodo, mp.nombre AS nombre_metodo,
                   ep.codigo AS codigo_estado, ep.nombre AS nombre_estado,
                   p.monto, p.moneda, p.referencia_externa, p.fecha_confirmacion,
                   c.fecha_hora_inicio AS fecha_cita,
                   CONCAT_WS(' ', paciente_persona.nombres, paciente_persona.apellidos) AS nombre_paciente,
                   paciente_persona.cedula::TEXT AS cedula_paciente,
                   CONCAT_WS(' ', medico_persona.nombres, medico_persona.apellidos) AS nombre_medico,
                   especialidad.nombre AS nombre_especialidad,
                   datos.correo AS correo_facturacion,
                   comprobante.id_comprobante, comprobante.numero_comprobante,
                   comprobante.estado_envio_email
            """ + RESUMEN_FROM;

    public List<MetodoPago> listarMetodosActivos() throws SQLException {
        String sql = """
                SELECT id_metodo_pago, codigo, nombre, descripcion, requiere_referencia
                  FROM soracare.metodos_pago
                 WHERE activo = TRUE
                 ORDER BY CASE codigo
                     WHEN 'QR' THEN 1 WHEN 'TRANSFERENCIA' THEN 2
                     WHEN 'TARJETA' THEN 3 WHEN 'EFECTIVO' THEN 4 ELSE 5 END
                """;
        List<MetodoPago> metodos = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                MetodoPago metodo = new MetodoPago();
                metodo.setIdMetodoPago(resultado.getInt("id_metodo_pago"));
                metodo.setCodigo(resultado.getString("codigo"));
                metodo.setNombre(resultado.getString("nombre"));
                metodo.setDescripcion(resultado.getString("descripcion"));
                metodo.setRequiereReferencia(resultado.getBoolean("requiere_referencia"));
                metodos.add(metodo);
            }
        }
        return metodos;
    }

    public BigDecimal buscarTarifa(Connection conexion, long idMedico, int idEspecialidad)
            throws SQLException {
        String sql = """
                SELECT tarifa_consulta
                  FROM soracare.medicos_especialidades
                 WHERE id_medico = ? AND id_especialidad = ? AND activo = TRUE
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idMedico);
            sentencia.setInt(2, idEspecialidad);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    throw new IllegalArgumentException(
                            "El medico no posee una tarifa activa para la especialidad seleccionada.");
                }
                return resultado.getBigDecimal("tarifa_consulta");
            }
        }
    }

    public long crearPendiente(
            Connection conexion,
            long idCita,
            String codigoMetodo,
            BigDecimal monto,
            OffsetDateTime fechaVencimiento,
            String referencia,
            String claveIdempotencia,
            DatosFacturacionDTO datos,
            long idUsuario
    ) throws SQLException {
        configurarAuditoria(conexion, idUsuario, "Creacion del pago de la cita");
        String sql = """
                INSERT INTO soracare.pagos (
                    id_cita, id_metodo_pago, id_estado_pago, monto, moneda,
                    proveedor_pago, referencia_externa, clave_idempotencia,
                    fecha_vencimiento, observacion
                )
                SELECT ?, metodo.id_metodo_pago, estado.id_estado_pago, ?, 'PYG',
                       ?, ?, ?, ?, 'Pago simulado de la etapa 5'
                  FROM soracare.metodos_pago metodo
                  CROSS JOIN soracare.estados_pago estado
                 WHERE metodo.codigo = ? AND metodo.activo = TRUE
                   AND estado.codigo = 'PENDIENTE'
                RETURNING id_pago
                """;
        long idPago;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idCita);
            sentencia.setBigDecimal(2, monto);
            sentencia.setString(3, "EFECTIVO".equals(codigoMetodo) ? null : "SIMULADOR_SORACARE");
            sentencia.setString(4, referencia);
            sentencia.setString(5, claveIdempotencia);
            sentencia.setObject(6, fechaVencimiento);
            sentencia.setString(7, codigoMetodo);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    throw new IllegalArgumentException("El metodo de pago seleccionado no esta disponible.");
                }
                idPago = resultado.getLong("id_pago");
            }
        }

        insertarDatosFacturacion(conexion, idPago, datos);
        if ("EFECTIVO".equals(codigoMetodo)) {
            programarConfirmacionAsistencia(conexion, idCita, fechaVencimiento);
        }
        return idPago;
    }

    private void insertarDatosFacturacion(Connection conexion, long idPago, DatosFacturacionDTO datos)
            throws SQLException {
        String sql = """
                INSERT INTO soracare.datos_facturacion_pago (
                    id_pago, tipo_documento, numero_documento,
                    nombre_razon_social, direccion, correo
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPago);
            sentencia.setString(2, datos.tipoDocumento());
            sentencia.setString(3, datos.numeroDocumento());
            sentencia.setString(4, datos.nombreRazonSocial());
            sentencia.setString(5, datos.direccion());
            sentencia.setString(6, datos.correo());
            sentencia.executeUpdate();
        }
    }

    private void programarConfirmacionAsistencia(
            Connection conexion, long idCita, OffsetDateTime fechaCita
    ) throws SQLException {
        String sql = """
                INSERT INTO soracare.confirmaciones_asistencia (
                    id_cita, fecha_programada_primer_aviso,
                    fecha_programada_segundo_aviso, fecha_vencimiento
                ) VALUES (?, ?, ?, ?)
                ON CONFLICT (id_cita) DO NOTHING
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idCita);
            sentencia.setObject(2, fechaCita.minusHours(24));
            sentencia.setObject(3, fechaCita.minusHours(18));
            sentencia.setObject(4, fechaCita.minusHours(12));
            sentencia.executeUpdate();
        }
    }

    public void reprogramarConfirmacionAsistencia(
            Connection conexion,
            long idCita,
            OffsetDateTime nuevaFechaCita
    ) throws SQLException {
        String sql = """
                UPDATE soracare.confirmaciones_asistencia
                   SET estado = 'PENDIENTE',
                       token_hash = NULL,
                       fecha_programada_primer_aviso = ?,
                       fecha_programada_segundo_aviso = ?,
                       fecha_primer_aviso = NULL,
                       fecha_segundo_aviso = NULL,
                       fecha_respuesta = NULL,
                       fecha_vencimiento = ?,
                       actualizado_en = CURRENT_TIMESTAMP
                 WHERE id_cita = ?
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setObject(1, nuevaFechaCita.minusHours(24));
            sentencia.setObject(2, nuevaFechaCita.minusHours(18));
            sentencia.setObject(3, nuevaFechaCita.minusHours(12));
            sentencia.setLong(4, idCita);
            sentencia.executeUpdate();
        }
    }

    public Optional<PagoResumenDTO> buscarPorId(long idPago) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            return buscarPorId(conexion, idPago);
        }
    }

    public Optional<PagoResumenDTO> buscarPorId(Connection conexion, long idPago) throws SQLException {
        return buscarUno(conexion, RESUMEN_SELECT + " WHERE p.id_pago = ?", idPago);
    }

    public DatosFacturacionDTO buscarDatosFacturacion(Connection conexion, long idPago)
            throws SQLException {
        String sql = """
                SELECT tipo_documento, numero_documento,
                       nombre_razon_social, direccion, correo
                  FROM soracare.datos_facturacion_pago
                 WHERE id_pago = ?
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPago);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException("El pago no posee datos de facturacion.");
                }
                return new DatosFacturacionDTO(
                        resultado.getString("tipo_documento"),
                        resultado.getString("numero_documento"),
                        resultado.getString("nombre_razon_social"),
                        resultado.getString("direccion"),
                        resultado.getString("correo")
                );
            }
        }
    }

    public List<PagoResumenDTO> listarPorPaciente(long idPaciente) throws SQLException {
        String sql = RESUMEN_SELECT + """
                 WHERE c.id_paciente = ?
                 ORDER BY p.creado_en DESC, p.id_pago DESC
                 LIMIT 150
                """;
        return listar(sql, idPaciente);
    }

    public List<PagoResumenDTO> listarRecientes() throws SQLException {
        String sql = RESUMEN_SELECT + """
                 ORDER BY CASE ep.codigo WHEN 'PENDIENTE' THEN 0 ELSE 1 END,
                          c.fecha_hora_inicio, p.id_pago DESC
                 LIMIT 300
                """;
        return listar(sql, null);
    }

    public ResultadoAgendaPagosDTO buscarParaAgenda(FiltroAgendaPagosDTO filtro)
            throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            long totalResultados = contarCoincidencias(conexion, filtro);
            int totalPaginas = totalResultados == 0
                    ? 0
                    : (int) Math.ceil((double) totalResultados / filtro.tamanoPagina());
            int paginaActual = totalPaginas == 0
                    ? 1
                    : Math.min(filtro.pagina(), totalPaginas);
            FiltroAgendaPagosDTO filtroPaginado = new FiltroAgendaPagosDTO(
                    filtro.cedula(), filtro.metodo(), filtro.estado(),
                    filtro.fechaDesde(), filtro.fechaHasta(), paginaActual,
                    filtro.tamanoPagina()
            );
            List<PagoResumenDTO> pagos = listarAgenda(conexion, filtroPaginado);
            long[] resumen = contarResumenOperativo(conexion);
            return new ResultadoAgendaPagosDTO(
                    pagos, totalResultados, paginaActual, totalPaginas,
                    filtro.tamanoPagina(), resumen[0], resumen[1], resumen[2]
            );
        }
    }

    private long contarCoincidencias(
            Connection conexion, FiltroAgendaPagosDTO filtro
    ) throws SQLException {
        String sql = "SELECT COUNT(*) " + RESUMEN_FROM + condicionesAgenda(filtro);
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            aplicarFiltrosAgenda(sentencia, filtro);
            try (ResultSet resultado = sentencia.executeQuery()) {
                resultado.next();
                return resultado.getLong(1);
            }
        }
    }

    private List<PagoResumenDTO> listarAgenda(
            Connection conexion, FiltroAgendaPagosDTO filtro
    ) throws SQLException {
        String sql = RESUMEN_SELECT + condicionesAgenda(filtro) + """
                 ORDER BY CASE ep.codigo WHEN 'PENDIENTE' THEN 0 ELSE 1 END,
                          c.fecha_hora_inicio, p.id_pago DESC
                 LIMIT ? OFFSET ?
                """;
        List<PagoResumenDTO> pagos = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            int indice = aplicarFiltrosAgenda(sentencia, filtro);
            sentencia.setInt(indice++, filtro.tamanoPagina());
            sentencia.setInt(indice, filtro.desplazamiento());
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) pagos.add(mapear(resultado));
            }
        }
        return pagos;
    }

    private long[] contarResumenOperativo(Connection conexion) throws SQLException {
        String sql = """
                SELECT COUNT(*) FILTER (WHERE ep.codigo = 'PENDIENTE') AS pendientes,
                       COUNT(*) FILTER (
                           WHERE ep.codigo = 'PENDIENTE' AND mp.codigo = 'EFECTIVO'
                       ) AS efectivo_pendiente,
                       COUNT(*) FILTER (
                           WHERE ep.codigo = 'PENDIENTE' AND mp.codigo <> 'EFECTIVO'
                       ) AS digital_pendiente
                  FROM soracare.pagos p
                  INNER JOIN soracare.metodos_pago mp
                          ON mp.id_metodo_pago = p.id_metodo_pago
                  INNER JOIN soracare.estados_pago ep
                          ON ep.id_estado_pago = p.id_estado_pago
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            resultado.next();
            return new long[]{
                    resultado.getLong("pendientes"),
                    resultado.getLong("efectivo_pendiente"),
                    resultado.getLong("digital_pendiente")
            };
        }
    }

    private static String condicionesAgenda(FiltroAgendaPagosDTO filtro) {
        StringBuilder condiciones = new StringBuilder(" WHERE 1 = 1");
        if (!filtro.cedula().isBlank()) {
            condiciones.append(" AND paciente_persona.cedula = ?");
        }
        if (!filtro.metodo().isBlank()) {
            condiciones.append(" AND mp.codigo = ?");
        }
        if (!filtro.estado().isBlank()) {
            condiciones.append(" AND ep.codigo = ?");
        }
        if (filtro.fechaDesde() != null) {
            condiciones.append(" AND c.fecha_hora_inicio >= ?");
        }
        if (filtro.fechaHasta() != null) {
            condiciones.append(" AND c.fecha_hora_inicio < ?");
        }
        return condiciones.toString();
    }

    private static int aplicarFiltrosAgenda(
            PreparedStatement sentencia, FiltroAgendaPagosDTO filtro
    ) throws SQLException {
        int indice = 1;
        if (!filtro.cedula().isBlank()) {
            sentencia.setLong(indice++, Long.parseLong(filtro.cedula()));
        }
        if (!filtro.metodo().isBlank()) {
            sentencia.setString(indice++, filtro.metodo());
        }
        if (!filtro.estado().isBlank()) {
            sentencia.setString(indice++, filtro.estado());
        }
        if (filtro.fechaDesde() != null) {
            sentencia.setObject(indice++, filtro.fechaDesde()
                    .atStartOfDay(ZONA_HORARIA).toOffsetDateTime());
        }
        if (filtro.fechaHasta() != null) {
            sentencia.setObject(indice++, filtro.fechaHasta().plusDays(1)
                    .atStartOfDay(ZONA_HORARIA).toOffsetDateTime());
        }
        return indice;
    }

    public void bloquear(Connection conexion, long idPago) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT id_pago FROM soracare.pagos WHERE id_pago = ? FOR UPDATE")) {
            sentencia.setLong(1, idPago);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) throw new IllegalArgumentException("El pago seleccionado no existe.");
            }
        }
    }

    public void confirmar(
            Connection conexion,
            long idPago,
            long idCita,
            long idUsuario,
            String referencia,
            String datosProveedor
    ) throws SQLException {
        configurarAuditoria(conexion, idUsuario, "Confirmacion simulada del pago");
        String sqlPago = """
                UPDATE soracare.pagos
                   SET id_estado_pago = (
                           SELECT id_estado_pago FROM soracare.estados_pago
                            WHERE codigo = 'CONFIRMADO'
                       ),
                       proveedor_pago = 'SIMULADOR_SORACARE',
                       referencia_externa = ?,
                       datos_proveedor = ?::jsonb,
                       observacion = 'Pago confirmado mediante simulacion academica'
                 WHERE id_pago = ?
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sqlPago)) {
            sentencia.setString(1, referencia);
            sentencia.setString(2, datosProveedor);
            sentencia.setLong(3, idPago);
            if (sentencia.executeUpdate() != 1) throw new SQLException("No fue posible confirmar el pago.");
        }

        String sqlCita = """
                UPDATE soracare.citas
                   SET id_estado_cita = (
                           SELECT id_estado_cita FROM soracare.estados_citas
                            WHERE codigo = 'CONFIRMADA'
                       ),
                       id_usuario_modificacion = ?
                 WHERE id_cita = ?
                   AND id_estado_cita NOT IN (
                       SELECT id_estado_cita FROM soracare.estados_citas
                        WHERE codigo IN ('CANCELADA', 'ATENDIDA')
                   )
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sqlCita)) {
            sentencia.setLong(1, idUsuario);
            sentencia.setLong(2, idCita);
            if (sentencia.executeUpdate() != 1) {
                throw new IllegalArgumentException("La cita ya no puede confirmarse.");
            }
        }
    }

    public long crearComprobante(
            Connection conexion,
            long idPago,
            long idUsuario,
            String numero,
            String nombreArchivo,
            byte[] contenido,
            String hash,
            String correo
    ) throws SQLException {
        String sql = """
                INSERT INTO soracare.comprobantes_pago (
                    id_pago, id_usuario_emisor, numero_comprobante,
                    tipo_comprobante, nombre_archivo, hash_sha256,
                    contenido_pdf, email_destino, enviado_email,
                    estado_envio_email, fecha_envio_email
                ) VALUES (?, ?, ?, 'RECIBO', ?, ?, ?, ?, TRUE,
                          'SIMULADO_ENVIADO', CURRENT_TIMESTAMP)
                RETURNING id_comprobante
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPago);
            sentencia.setLong(2, idUsuario);
            sentencia.setString(3, numero);
            sentencia.setString(4, nombreArchivo);
            sentencia.setString(5, hash);
            sentencia.setBytes(6, contenido);
            sentencia.setString(7, correo);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) throw new SQLException("No fue posible crear el comprobante.");
                return resultado.getLong("id_comprobante");
            }
        }
    }

    public void registrarCorreoSimulado(
            Connection conexion,
            PagoResumenDTO pago,
            String numeroComprobante
    ) throws SQLException {
        String sql = """
                INSERT INTO soracare.notificaciones (
                    id_persona, id_cita, id_pago, id_plantilla_notificacion,
                    id_canal_notificacion, id_estado_notificacion,
                    destinatario, asunto, contenido, clave_idempotencia,
                    fecha_envio, proveedor, referencia_proveedor
                )
                SELECT paciente.id_persona, ?, ?, plantilla.id_plantilla_notificacion,
                       canal.id_canal_notificacion, estado.id_estado_notificacion,
                       ?, 'Comprobante de pago - SoraCare-FPUNE', ?, ?,
                       CURRENT_TIMESTAMP, 'SIMULADOR_LOCAL', ?
                  FROM soracare.citas cita
                  INNER JOIN soracare.pacientes paciente
                          ON paciente.id_paciente = cita.id_paciente
                  CROSS JOIN soracare.canales_notificacion canal
                  CROSS JOIN soracare.estados_notificacion estado
                  LEFT JOIN soracare.plantillas_notificacion plantilla
                         ON plantilla.codigo = 'PAGO_CONFIRMADO_EMAIL'
                 WHERE cita.id_cita = ?
                   AND canal.codigo = 'EMAIL'
                   AND estado.codigo = 'ENVIADA'
                ON CONFLICT (clave_idempotencia) DO NOTHING
                """;
        String contenido = "Pago simulado confirmado por " + pago.monto().toPlainString()
                + " PYG. Comprobante " + numeroComprobante
                + ". Documento academico sin validez fiscal.";
        String clave = "PAGO-" + pago.idPago() + "-EMAIL-1";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, pago.idCita());
            sentencia.setLong(2, pago.idPago());
            sentencia.setString(3, pago.correoFacturacion());
            sentencia.setString(4, contenido);
            sentencia.setString(5, clave);
            sentencia.setString(6, numeroComprobante);
            sentencia.setLong(7, pago.idCita());
            sentencia.executeUpdate();
        }
    }

    public void resolverCancelacion(
            Connection conexion, long idCita, long idUsuario
    ) throws SQLException {
        configurarAuditoria(conexion, idUsuario, "Cancelacion de la cita asociada");
        String sql = """
                UPDATE soracare.pagos pago
                   SET id_estado_pago = destino.id_estado_pago,
                       observacion = CASE origen.codigo
                           WHEN 'CONFIRMADO' THEN 'Reembolso simulado por cancelacion de cita'
                           ELSE 'Pago anulado por cancelacion de cita' END
                  FROM soracare.estados_pago origen,
                       soracare.estados_pago destino
                 WHERE pago.id_cita = ?
                   AND origen.id_estado_pago = pago.id_estado_pago
                   AND origen.codigo IN ('PENDIENTE', 'PROCESANDO', 'CONFIRMADO')
                   AND destino.codigo = CASE origen.codigo
                       WHEN 'CONFIRMADO' THEN 'REEMBOLSADO' ELSE 'ANULADO' END
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idCita);
            sentencia.executeUpdate();
        }
    }

    public Optional<ComprobantePagoDTO> buscarComprobante(long idComprobante) throws SQLException {
        String sql = """
                SELECT comprobante.id_comprobante, comprobante.id_pago,
                       cita.id_paciente, comprobante.nombre_archivo,
                       comprobante.contenido_pdf
                  FROM soracare.comprobantes_pago comprobante
                  INNER JOIN soracare.pagos pago ON pago.id_pago = comprobante.id_pago
                  INNER JOIN soracare.citas cita ON cita.id_cita = pago.id_cita
                 WHERE comprobante.id_comprobante = ?
                """;
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idComprobante);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) return Optional.empty();
                return Optional.of(new ComprobantePagoDTO(
                        resultado.getLong("id_comprobante"),
                        resultado.getLong("id_pago"),
                        resultado.getLong("id_paciente"),
                        resultado.getString("nombre_archivo"),
                        resultado.getBytes("contenido_pdf")
                ));
            }
        }
    }

    private Optional<PagoResumenDTO> buscarUno(Connection conexion, String sql, long id)
            throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, id);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        }
    }

    private List<PagoResumenDTO> listar(String sql, Long idPaciente) throws SQLException {
        List<PagoResumenDTO> pagos = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            if (idPaciente != null) sentencia.setLong(1, idPaciente);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) pagos.add(mapear(resultado));
            }
        }
        return pagos;
    }

    private static PagoResumenDTO mapear(ResultSet resultado) throws SQLException {
        Object idComprobante = resultado.getObject("id_comprobante");
        return new PagoResumenDTO(
                resultado.getLong("id_pago"),
                resultado.getLong("id_cita"),
                resultado.getLong("id_paciente"),
                resultado.getString("codigo_metodo"),
                resultado.getString("nombre_metodo"),
                resultado.getString("codigo_estado"),
                resultado.getString("nombre_estado"),
                resultado.getBigDecimal("monto"),
                resultado.getString("moneda"),
                resultado.getString("referencia_externa"),
                resultado.getObject("fecha_confirmacion", OffsetDateTime.class),
                resultado.getObject("fecha_cita", OffsetDateTime.class),
                resultado.getString("nombre_paciente"),
                resultado.getString("cedula_paciente"),
                resultado.getString("nombre_medico"),
                resultado.getString("nombre_especialidad"),
                resultado.getString("correo_facturacion"),
                idComprobante == null ? null : ((Number) idComprobante).longValue(),
                resultado.getString("numero_comprobante"),
                resultado.getString("estado_envio_email")
        );
    }

    private static void configurarAuditoria(
            Connection conexion, long idUsuario, String motivo
    ) throws SQLException {
        String sql = "SELECT set_config('soracare.usuario_id', ?, TRUE), "
                + "set_config('soracare.motivo_pago', ?, TRUE)";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, Long.toString(idUsuario));
            sentencia.setString(2, motivo);
            sentencia.execute();
        }
    }
}
