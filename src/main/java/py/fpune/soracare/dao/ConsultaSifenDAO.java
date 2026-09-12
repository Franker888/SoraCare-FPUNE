package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.ConsultaSifenHistorialDTO;
import py.fpune.soracare.sifen.ResultadoConsultaSifen;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Historial separado de la emisión: nunca actualiza solicitudes_sifen. */
public class ConsultaSifenDAO {
    public void iniciar(long id, long solicitud, long medico, String cdc) throws SQLException {
        try (var c = ConexionBD.obtenerConexion()) { iniciar(c, id, solicitud, medico, cdc); }
    }

    public void iniciar(Connection c, long id, long solicitud, long medico, String cdc) throws SQLException {
        try (var p = c.prepareStatement("""
                INSERT INTO soracare.consultas_sifen(id_consulta, id_solicitud, id_persona_medico, cdc)
                SELECT ?, s.id_solicitud, m.id_persona, s.cdc
                FROM soracare.solicitudes_sifen s
                JOIN soracare.facturas_internas f ON f.id_factura = s.id_factura_interna
                JOIN soracare.pagos p ON p.id_pago = f.id_pago
                JOIN soracare.citas c ON c.id_cita = p.id_cita
                JOIN soracare.medicos m ON m.id_medico = c.id_medico
                WHERE s.id_solicitud = ? AND m.id_persona = ? AND s.cdc = ? AND s.ambiente = 'PRUEBA'
                """)) {
            p.setLong(1, id); p.setLong(2, solicitud); p.setLong(3, medico); p.setString(4, cdc);
            if (p.executeUpdate() != 1) throw new SQLException("No fue posible registrar la consulta propia.");
        }
    }

    public void finalizar(long id, ResultadoConsultaSifen resultado) throws SQLException {
        try (var c = ConexionBD.obtenerConexion()) { finalizar(c, id, resultado); }
    }

    public void finalizar(Connection c, long id, ResultadoConsultaSifen r) throws SQLException {
        try (var p = c.prepareStatement("""
                UPDATE soracare.consultas_sifen SET terminado_en = CURRENT_TIMESTAMP,
                    estado = ?, codigo = ?, mensaje = ?, protocolo = ?, eventos = ?, observacion_eventos = ?
                WHERE id_consulta = ? AND estado = 'SIN_RESULTADO' AND cdc = ?
                """)) {
            p.setString(1, r.estado().name()); p.setString(2, r.codigo()); p.setString(3, r.mensaje());
            p.setString(4, r.protocolo()); p.setInt(5, r.eventos()); p.setString(6, r.observacionEventos());
            p.setLong(7, id); p.setString(8, r.cdc());
            if (p.executeUpdate() != 1) throw new SQLException("No fue posible cerrar el registro de consulta.");
        }
    }

    public List<ConsultaSifenHistorialDTO> listarDelMedico(long medico) throws SQLException {
        try (var c = ConexionBD.obtenerConexion()) { return listarDelMedico(c, medico); }
    }

    public List<ConsultaSifenHistorialDTO> listarDelMedico(Connection c, long medico) throws SQLException {
        try (var p = c.prepareStatement("""
                SELECT h.* FROM soracare.consultas_sifen h
                JOIN soracare.solicitudes_sifen s ON s.id_solicitud = h.id_solicitud
                JOIN soracare.facturas_internas f ON f.id_factura = s.id_factura_interna
                JOIN soracare.pagos p ON p.id_pago = f.id_pago
                JOIN soracare.citas c ON c.id_cita = p.id_cita
                JOIN soracare.medicos m ON m.id_medico = c.id_medico
                WHERE h.id_persona_medico = ? AND m.id_persona = ?
                ORDER BY h.id_consulta DESC LIMIT 50
                """)) {
            p.setLong(1, medico); p.setLong(2, medico);
            var lista = new ArrayList<ConsultaSifenHistorialDTO>();
            try (var r = p.executeQuery()) {
                while (r.next()) lista.add(new ConsultaSifenHistorialDTO(r.getLong("id_consulta"),
                        r.getLong("id_solicitud"), r.getString("cdc"), r.getObject("iniciado_en", OffsetDateTime.class),
                        r.getString("estado"), r.getString("codigo"), r.getString("mensaje"),
                        r.getString("protocolo"), r.getObject("eventos", Integer.class), r.getString("observacion_eventos")));
            }
            return List.copyOf(lista);
        }
    }
}
