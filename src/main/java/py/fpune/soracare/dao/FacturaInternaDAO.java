package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.DatosFacturacionDTO;
import py.fpune.soracare.dto.FacturaInternaArchivoDTO;
import py.fpune.soracare.dto.FacturaInternaCreadaDTO;
import py.fpune.soracare.dto.PagoResumenDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

public class FacturaInternaDAO {

    private static final Set<String> ESTADOS_CORREO = Set.of(
            "PENDIENTE", "ENVIADO", "FALLIDO", "NO_CONFIGURADO"
    );

    public boolean perteneceAlMedico(
            Connection conexion,
            long idPago,
            long idPersonaMedico
    ) throws SQLException {
        String sql = """
                SELECT 1
                  FROM soracare.pagos pago
                  INNER JOIN soracare.citas cita ON cita.id_cita = pago.id_cita
                  INNER JOIN soracare.medicos medico ON medico.id_medico = cita.id_medico
                 WHERE pago.id_pago = ? AND medico.id_persona = ?
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPago);
            sentencia.setLong(2, idPersonaMedico);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next();
            }
        }
    }

    public Optional<FacturaInternaCreadaDTO> buscarPorPago(
            Connection conexion,
            long idPago
    ) throws SQLException {
        String sql = """
                SELECT id_factura, id_pago, numero_factura, estado_envio_email
                  FROM soracare.facturas_internas
                 WHERE id_pago = ?
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPago);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next()
                        ? Optional.of(mapearCreada(resultado))
                        : Optional.empty();
            }
        }
    }

    public FacturaInternaCreadaDTO crear(
            Connection conexion,
            PagoResumenDTO pago,
            DatosFacturacionDTO facturacion,
            long idUsuario
    ) throws SQLException {
        String descripcion = "Consulta medica - " + pago.nombreEspecialidad();
        String insertarFactura = """
                INSERT INTO soracare.facturas_internas (
                    id_pago, id_usuario_emisor,
                    tipo_documento, numero_documento, nombre_razon_social,
                    direccion, correo, subtotal, total, moneda, descripcion
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id_factura, id_pago, numero_factura, estado_envio_email
                """;
        FacturaInternaCreadaDTO factura;
        try (PreparedStatement sentencia = conexion.prepareStatement(insertarFactura)) {
            sentencia.setLong(1, pago.idPago());
            sentencia.setLong(2, idUsuario);
            sentencia.setString(3, facturacion.tipoDocumento());
            sentencia.setString(4, facturacion.numeroDocumento());
            sentencia.setString(5, facturacion.nombreRazonSocial());
            sentencia.setString(6, facturacion.direccion());
            sentencia.setString(7, facturacion.correo());
            sentencia.setBigDecimal(8, pago.monto());
            sentencia.setBigDecimal(9, pago.monto());
            sentencia.setString(10, pago.moneda());
            sentencia.setString(11, descripcion);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException("No fue posible crear la factura interna.");
                }
                factura = mapearCreada(resultado);
            }
        }

        String insertarDetalle = """
                INSERT INTO soracare.facturas_internas_detalles (
                    id_factura, descripcion, cantidad, precio_unitario, total_linea
                ) VALUES (?, ?, 1, ?, ?)
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(insertarDetalle)) {
            sentencia.setLong(1, factura.idFactura());
            sentencia.setString(2, descripcion);
            sentencia.setBigDecimal(3, pago.monto());
            sentencia.setBigDecimal(4, pago.monto());
            sentencia.executeUpdate();
        }
        return factura;
    }

    public void guardarDocumento(
            Connection conexion,
            long idFactura,
            String nombreArchivo,
            byte[] contenido,
            String hash
    ) throws SQLException {
        String sql = """
                UPDATE soracare.facturas_internas
                   SET nombre_archivo = ?, contenido_pdf = ?, hash_sha256 = ?,
                       actualizado_en = CURRENT_TIMESTAMP
                 WHERE id_factura = ?
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, nombreArchivo);
            sentencia.setBytes(2, contenido);
            sentencia.setString(3, hash);
            sentencia.setLong(4, idFactura);
            if (sentencia.executeUpdate() != 1) {
                throw new SQLException("No fue posible guardar el PDF de la factura interna.");
            }
        }
    }

    public Optional<FacturaInternaArchivoDTO> buscarArchivo(long idFactura)
            throws SQLException {
        String sql = """
                SELECT factura.id_factura, factura.id_pago, factura.numero_factura,
                       factura.nombre_archivo, factura.contenido_pdf,
                       paciente.id_persona AS id_persona_paciente,
                       medico.id_persona AS id_persona_medico
                  FROM soracare.facturas_internas factura
                  INNER JOIN soracare.pagos pago ON pago.id_pago = factura.id_pago
                  INNER JOIN soracare.citas cita ON cita.id_cita = pago.id_cita
                  INNER JOIN soracare.pacientes paciente
                          ON paciente.id_paciente = cita.id_paciente
                  INNER JOIN soracare.medicos medico ON medico.id_medico = cita.id_medico
                 WHERE factura.id_factura = ?
                """;
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idFactura);
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) return Optional.empty();
                return Optional.of(new FacturaInternaArchivoDTO(
                        resultado.getLong("id_factura"),
                        resultado.getLong("id_pago"),
                        resultado.getLong("id_persona_paciente"),
                        resultado.getLong("id_persona_medico"),
                        resultado.getString("numero_factura"),
                        resultado.getString("nombre_archivo"),
                        resultado.getBytes("contenido_pdf")
                ));
            }
        }
    }

    public void registrarResultadoCorreo(long idFactura, String estadoCorreo)
            throws SQLException {
        if (!ESTADOS_CORREO.contains(estadoCorreo)) {
            throw new IllegalArgumentException("El estado del correo no es valido.");
        }
        String sql = """
                UPDATE soracare.facturas_internas
                   SET estado_envio_email = ?,
                       fecha_envio_email = CASE
                           WHEN ? = 'ENVIADO' THEN CURRENT_TIMESTAMP
                           ELSE NULL
                       END,
                       actualizado_en = CURRENT_TIMESTAMP
                 WHERE id_factura = ?
                """;
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, estadoCorreo);
            sentencia.setString(2, estadoCorreo);
            sentencia.setLong(3, idFactura);
            if (sentencia.executeUpdate() != 1) {
                throw new SQLException("No fue posible actualizar el correo de la factura interna.");
            }
        }
    }

    private static FacturaInternaCreadaDTO mapearCreada(ResultSet resultado)
            throws SQLException {
        return new FacturaInternaCreadaDTO(
                resultado.getLong("id_factura"),
                resultado.getLong("id_pago"),
                resultado.getString("numero_factura"),
                resultado.getString("estado_envio_email")
        );
    }
}
