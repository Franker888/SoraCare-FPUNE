package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.FacturaParaSifenDTO;
import py.fpune.soracare.dto.DocumentoSifenEnvioDTO;
import py.fpune.soracare.dto.SolicitudSifenResumenDTO;
import py.fpune.soracare.sifen.AfectacionIvaSifen;
import py.fpune.soracare.sifen.ResultadoFiscalSifen;
import py.fpune.soracare.sifen.RespuestaRecepcionSifen;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SolicitudSifenDAO {

    public Optional<py.fpune.soracare.dto.DocumentoKudeDTO> buscarKudeDelMedico(
            long idSolicitud, long idPersonaMedico) throws SQLException {
        String sql = """
                SELECT s.ambiente, s.estado, s.cdc, s.xml_firmado,
                       s.hash_xml_sha256, s.xsd_validado
                  FROM soracare.solicitudes_sifen s
                  JOIN soracare.facturas_internas f ON f.id_factura = s.id_factura_interna
                  JOIN soracare.pagos p ON p.id_pago = f.id_pago
                  JOIN soracare.citas c ON c.id_cita = p.id_cita
                  JOIN soracare.medicos m ON m.id_medico = c.id_medico
                 WHERE s.id_solicitud = ? AND m.id_persona = ?
                """;
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idSolicitud);
            sentencia.setLong(2, idPersonaMedico);
            try (ResultSet r = sentencia.executeQuery()) {
                if (!r.next()) return Optional.empty();
                return Optional.of(new py.fpune.soracare.dto.DocumentoKudeDTO(
                        r.getString("ambiente"), r.getString("estado"), r.getString("cdc"),
                        r.getBytes("xml_firmado"), r.getString("hash_xml_sha256"),
                        r.getBoolean("xsd_validado")));
            }
        }
    }

    public Optional<FacturaParaSifenDTO> buscarFacturaDelMedico(
            Connection conexion,
            long idFactura,
            long idPersonaMedico
    ) throws SQLException {
        String sql = """
                SELECT factura.id_factura, factura.id_pago,
                       factura.numero_factura, factura.tipo_documento,
                       factura.numero_documento, factura.nombre_razon_social,
                       factura.correo, factura.total, factura.moneda,
                       factura.fecha_emision
                  FROM soracare.facturas_internas factura
                  INNER JOIN soracare.pagos pago ON pago.id_pago = factura.id_pago
                  INNER JOIN soracare.citas cita ON cita.id_cita = pago.id_cita
                  INNER JOIN soracare.medicos medico ON medico.id_medico = cita.id_medico
                 WHERE factura.id_factura = ?
                   AND medico.id_persona = ?
                   AND factura.estado = 'EMITIDA'
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idFactura);
            sentencia.setLong(2, idPersonaMedico);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) return Optional.empty();
                return Optional.of(new FacturaParaSifenDTO(
                        resultado.getLong("id_factura"),
                        resultado.getLong("id_pago"),
                        resultado.getString("numero_factura"),
                        resultado.getString("tipo_documento"),
                        resultado.getString("numero_documento"),
                        resultado.getString("nombre_razon_social"),
                        resultado.getString("correo"),
                        resultado.getBigDecimal("total"),
                        resultado.getString("moneda"),
                        resultado.getObject("fecha_emision", OffsetDateTime.class),
                        buscarDetalles(conexion, idFactura)
                ));
            }
        }
    }

    public Optional<SolicitudSifenResumenDTO> buscarPorFactura(
            Connection conexion,
            long idFactura
    ) throws SQLException {
        String sql = SELECT_RESUMEN + " WHERE id_factura_interna = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idFactura);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next()
                        ? Optional.of(mapear(resultado))
                        : Optional.empty();
            }
        }
    }

    public Map<Long, SolicitudSifenResumenDTO> listarPorMedico(
            long idPersonaMedico
    ) throws SQLException {
        String sql = """
                SELECT solicitud.id_solicitud, solicitud.id_factura_interna,
                       solicitud.ambiente, solicitud.estado,
                       solicitud.numero_documento, solicitud.cdc,
                       solicitud.xsd_validado, solicitud.codigo_respuesta,
                       solicitud.mensaje_respuesta, solicitud.url_qr,
                       solicitud.actualizado_en
                  FROM soracare.solicitudes_sifen solicitud
                  INNER JOIN soracare.facturas_internas factura
                          ON factura.id_factura = solicitud.id_factura_interna
                  INNER JOIN soracare.pagos pago ON pago.id_pago = factura.id_pago
                  INNER JOIN soracare.citas cita ON cita.id_cita = pago.id_cita
                  INNER JOIN soracare.medicos medico ON medico.id_medico = cita.id_medico
                 WHERE medico.id_persona = ?
                 ORDER BY solicitud.actualizado_en DESC
                """;
        Map<Long, SolicitudSifenResumenDTO> solicitudes = new LinkedHashMap<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPersonaMedico);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    SolicitudSifenResumenDTO solicitud = mapear(resultado);
                    solicitudes.put(solicitud.idFacturaInterna(), solicitud);
                }
            }
        }
        return Map.copyOf(solicitudes);
    }

    public Optional<DocumentoSifenEnvioDTO> buscarParaEnvio(
            Connection conexion,
            long idSolicitud,
            long idPersonaMedico
    ) throws SQLException {
        String sql = """
                SELECT solicitud.id_solicitud, solicitud.id_factura_interna,
                       solicitud.ambiente, solicitud.estado, solicitud.cdc,
                       solicitud.xml_firmado
                  FROM soracare.solicitudes_sifen solicitud
                  INNER JOIN soracare.facturas_internas factura
                          ON factura.id_factura = solicitud.id_factura_interna
                  INNER JOIN soracare.pagos pago ON pago.id_pago = factura.id_pago
                  INNER JOIN soracare.citas cita ON cita.id_cita = pago.id_cita
                  INNER JOIN soracare.medicos medico ON medico.id_medico = cita.id_medico
                 WHERE solicitud.id_solicitud = ?
                   AND medico.id_persona = ?
                 FOR UPDATE OF solicitud
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idSolicitud);
            sentencia.setLong(2, idPersonaMedico);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) return Optional.empty();
                return Optional.of(new DocumentoSifenEnvioDTO(
                        resultado.getLong("id_solicitud"),
                        resultado.getLong("id_factura_interna"),
                        resultado.getString("ambiente"),
                        resultado.getString("estado"),
                        resultado.getString("cdc"),
                        resultado.getBytes("xml_firmado")
                ));
            }
        }
    }

    public void marcarEnviado(
            Connection conexion,
            long idSolicitud,
            long identificadorEnvio
    ) throws SQLException {
        String sql = """
                UPDATE soracare.solicitudes_sifen
                   SET estado = 'ENVIADO', identificador_envio = ?,
                       intentos = intentos + 1,
                       enviado_en = CURRENT_TIMESTAMP,
                       actualizado_en = CURRENT_TIMESTAMP
                 WHERE id_solicitud = ?
                   AND estado IN ('VALIDADO', 'ERROR')
                   AND xsd_validado = TRUE
                   AND xml_firmado IS NOT NULL
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, identificadorEnvio);
            sentencia.setLong(2, idSolicitud);
            if (sentencia.executeUpdate() != 1) {
                throw new IllegalArgumentException(
                        "El DE no está disponible para un nuevo envío."
                );
            }
        }
    }

    public long siguienteIdentificadorEnvio(Connection conexion)
            throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT NEXTVAL('soracare.sifen_identificador_envio_seq')"
        ); ResultSet resultado = sentencia.executeQuery()) {
            if (!resultado.next()) {
                throw new SQLException(
                        "No fue posible obtener el identificador de envío SIFEN."
                );
            }
            return resultado.getLong(1);
        }
    }

    public SolicitudSifenResumenDTO guardarRespuesta(
            Connection conexion,
            long idSolicitud,
            RespuestaRecepcionSifen respuesta
    ) throws SQLException {
        String sql = """
                UPDATE soracare.solicitudes_sifen
                   SET estado = ?, protocolo_autorizacion = ?,
                       codigo_respuesta = ?, mensaje_respuesta = ?,
                       codigo_http = ?, respuesta_xml = ?,
                       respondido_en = CURRENT_TIMESTAMP,
                       actualizado_en = CURRENT_TIMESTAMP
                 WHERE id_solicitud = ? AND estado = 'ENVIADO'
                RETURNING id_solicitud, id_factura_interna, ambiente, estado,
                          numero_documento, cdc, xsd_validado, codigo_respuesta,
                          mensaje_respuesta, url_qr, actualizado_en
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, respuesta.estado().name());
            sentencia.setString(2, respuesta.protocolo());
            sentencia.setString(3, respuesta.codigo());
            sentencia.setString(4, respuesta.mensaje());
            sentencia.setInt(5, respuesta.codigoHttp());
            sentencia.setBytes(6, respuesta.contenido());
            sentencia.setLong(7, idSolicitud);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException("No fue posible guardar la respuesta de SIFEN.");
                }
                return mapear(resultado);
            }
        }
    }

    public void guardarErrorTransporte(long idSolicitud, String mensaje)
            throws SQLException {
        String sql = """
                UPDATE soracare.solicitudes_sifen
                   SET estado = 'ERROR', codigo_respuesta = 'TRANSPORTE',
                       mensaje_respuesta = ?, respondido_en = CURRENT_TIMESTAMP,
                       actualizado_en = CURRENT_TIMESTAMP
                 WHERE id_solicitud = ? AND estado = 'ENVIADO'
                """;
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, limitar(mensaje));
            sentencia.setLong(2, idSolicitud);
            sentencia.executeUpdate();
        }
    }

    public String reservarNumero(
            Connection conexion,
            String ambiente,
            String establecimiento,
            String puntoExpedicion,
            String serie
    ) throws SQLException {
        String normalizadoEstablecimiento = completar(establecimiento, 3);
        String normalizadoPunto = completar(puntoExpedicion, 3);
        String normalizadoSerie = serie == null ? "" : serie.strip().toUpperCase();
        try (PreparedStatement insertar = conexion.prepareStatement("""
                INSERT INTO soracare.numeraciones_sifen (
                    ambiente, tipo_documento, establecimiento,
                    punto_expedicion, serie, siguiente_numero
                ) VALUES (?, 1, ?, ?, ?, 1)
                ON CONFLICT DO NOTHING
                """)) {
            insertar.setString(1, ambiente);
            insertar.setString(2, normalizadoEstablecimiento);
            insertar.setString(3, normalizadoPunto);
            insertar.setString(4, normalizadoSerie);
            insertar.executeUpdate();
        }

        int siguiente;
        try (PreparedStatement bloquear = conexion.prepareStatement("""
                SELECT siguiente_numero
                  FROM soracare.numeraciones_sifen
                 WHERE ambiente = ? AND tipo_documento = 1
                   AND establecimiento = ? AND punto_expedicion = ? AND serie = ?
                 FOR UPDATE
                """)) {
            bloquear.setString(1, ambiente);
            bloquear.setString(2, normalizadoEstablecimiento);
            bloquear.setString(3, normalizadoPunto);
            bloquear.setString(4, normalizadoSerie);
            try (ResultSet resultado = bloquear.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException("No fue posible reservar la numeración SIFEN.");
                }
                siguiente = resultado.getInt(1);
            }
        }
        if (siguiente > 9_999_999) {
            throw new SQLException("La numeración SIFEN configurada está agotada.");
        }
        try (PreparedStatement actualizar = conexion.prepareStatement("""
                UPDATE soracare.numeraciones_sifen
                   SET siguiente_numero = ?, actualizado_en = CURRENT_TIMESTAMP
                 WHERE ambiente = ? AND tipo_documento = 1
                   AND establecimiento = ? AND punto_expedicion = ? AND serie = ?
                """)) {
            actualizar.setInt(1, siguiente + 1);
            actualizar.setString(2, ambiente);
            actualizar.setString(3, normalizadoEstablecimiento);
            actualizar.setString(4, normalizadoPunto);
            actualizar.setString(5, normalizadoSerie);
            if (actualizar.executeUpdate() != 1) {
                throw new SQLException("No fue posible actualizar la numeración SIFEN.");
            }
        }
        return "%07d".formatted(siguiente);
    }

    public SolicitudSifenResumenDTO guardarValidada(
            Connection conexion,
            long idFactura,
            String ambiente,
            String establecimiento,
            String puntoExpedicion,
            String numeroDocumento,
            String codigoSeguridad,
            String cdc,
            AfectacionIvaSifen afectacion,
            int tasaIva,
            ResultadoFiscalSifen fiscal,
            byte[] xmlSinFirma,
            byte[] xmlFirmado,
            String urlQr,
            String hashXml
    ) throws SQLException {
        String sql = """
                INSERT INTO soracare.solicitudes_sifen (
                    id_factura_interna, ambiente, estado,
                    establecimiento, punto_expedicion, serie,
                    numero_documento, codigo_seguridad, cdc, dv_cdc,
                    afectacion_iva, tasa_iva, total_operacion, total_iva,
                    xml_sin_firma, xml_firmado, url_qr, xsd_validado,
                    hash_xml_sha256, firmado_en, actualizado_en
                ) VALUES (
                    ?, ?, 'VALIDADO', ?, ?, '', ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, TRUE, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                RETURNING id_solicitud, id_factura_interna, ambiente, estado,
                          numero_documento, cdc, xsd_validado, codigo_respuesta,
                          mensaje_respuesta, url_qr, actualizado_en
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            int indice = 1;
            sentencia.setLong(indice++, idFactura);
            sentencia.setString(indice++, ambiente);
            sentencia.setString(indice++, completar(establecimiento, 3));
            sentencia.setString(indice++, completar(puntoExpedicion, 3));
            sentencia.setString(indice++, numeroDocumento);
            sentencia.setString(indice++, codigoSeguridad);
            sentencia.setString(indice++, cdc);
            sentencia.setInt(indice++, Character.digit(cdc.charAt(43), 10));
            sentencia.setInt(indice++, afectacion.codigo());
            sentencia.setInt(indice++, tasaIva);
            sentencia.setBigDecimal(indice++, fiscal.totalOperacion());
            sentencia.setBigDecimal(indice++, fiscal.totalIva());
            sentencia.setBytes(indice++, xmlSinFirma);
            sentencia.setBytes(indice++, xmlFirmado);
            sentencia.setString(indice++, urlQr);
            sentencia.setString(indice, hashXml);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException("No fue posible guardar el documento SIFEN.");
                }
                return mapear(resultado);
            }
        }
    }

    private static List<FacturaParaSifenDTO.Detalle> buscarDetalles(
            Connection conexion,
            long idFactura
    ) throws SQLException {
        String sql = """
                SELECT id_detalle, descripcion, cantidad, precio_unitario
                  FROM soracare.facturas_internas_detalles
                 WHERE id_factura = ?
                 ORDER BY id_detalle
                """;
        List<FacturaParaSifenDTO.Detalle> detalles = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idFactura);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    detalles.add(new FacturaParaSifenDTO.Detalle(
                            resultado.getLong("id_detalle"),
                            resultado.getString("descripcion"),
                            resultado.getBigDecimal("cantidad"),
                            resultado.getBigDecimal("precio_unitario")
                    ));
                }
            }
        }
        return detalles;
    }

    private static SolicitudSifenResumenDTO mapear(ResultSet resultado)
            throws SQLException {
        return new SolicitudSifenResumenDTO(
                resultado.getLong("id_solicitud"),
                resultado.getLong("id_factura_interna"),
                resultado.getString("ambiente"),
                resultado.getString("estado"),
                resultado.getString("numero_documento"),
                resultado.getString("cdc"),
                resultado.getBoolean("xsd_validado"),
                resultado.getString("codigo_respuesta"),
                resultado.getString("mensaje_respuesta"),
                resultado.getString("url_qr"),
                resultado.getObject("actualizado_en", OffsetDateTime.class)
        );
    }

    private static String completar(String valor, int longitud) {
        String limpio = valor == null ? "" : valor.strip();
        if (!limpio.matches("[0-9]+") || limpio.length() > longitud) {
            throw new IllegalArgumentException("La numeración SIFEN no es válida.");
        }
        return "0".repeat(longitud - limpio.length()) + limpio;
    }

    private static String limitar(String valor) {
        String limpio = valor == null || valor.isBlank()
                ? "Error de transporte sin detalle."
                : valor.strip();
        return limpio.length() <= 1000 ? limpio : limpio.substring(0, 1000);
    }

    private static final String SELECT_RESUMEN = """
            SELECT id_solicitud, id_factura_interna, ambiente, estado,
                   numero_documento, cdc, xsd_validado, codigo_respuesta,
                   mensaje_respuesta, url_qr, actualizado_en
              FROM soracare.solicitudes_sifen
            """;
}
