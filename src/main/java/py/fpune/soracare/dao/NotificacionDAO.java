package py.fpune.soracare.dao;

import py.fpune.soracare.dto.NotificacionDTO;
import py.fpune.soracare.dto.RecetaMedicaDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificacionDAO {

    public void registrarEventoCita(
            Connection conexion,
            long idCita,
            String codigoPlantilla,
            String claveEvento,
            String asunto,
            String contenido
    ) throws SQLException {
        String sql = """
                INSERT INTO soracare.notificaciones (
                    id_persona, id_cita, id_plantilla_notificacion,
                    id_canal_notificacion, id_estado_notificacion,
                    destinatario, asunto, contenido, clave_idempotencia,
                    fecha_envio, fecha_entrega, proveedor, referencia_proveedor
                )
                SELECT paciente.id_persona, cita.id_cita,
                       plantilla.id_plantilla_notificacion,
                       canal.id_canal_notificacion, estado.id_estado_notificacion,
                       'PORTAL:' || paciente.id_persona,
                       ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                       'SORACARE_INTERNO', ?
                  FROM soracare.citas cita
                  INNER JOIN soracare.pacientes paciente
                          ON paciente.id_paciente = cita.id_paciente
                  CROSS JOIN soracare.canales_notificacion canal
                  CROSS JOIN soracare.estados_notificacion estado
                  LEFT JOIN soracare.plantillas_notificacion plantilla
                         ON plantilla.codigo = ?
                 WHERE cita.id_cita = ?
                   AND canal.codigo = 'SISTEMA'
                   AND estado.codigo = 'ENTREGADA'
                ON CONFLICT (clave_idempotencia) DO NOTHING
                """;
        String clave = "CITA-" + idCita + "-" + claveEvento;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, asunto);
            sentencia.setString(2, contenido);
            sentencia.setString(3, clave);
            sentencia.setString(4, claveEvento);
            sentencia.setString(5, codigoPlantilla);
            sentencia.setLong(6, idCita);
            sentencia.executeUpdate();
        }
    }

    public void registrarPagoConfirmado(
            Connection conexion,
            long idCita,
            long idPago,
            String numeroComprobante,
            String contenido
    ) throws SQLException {
        String sql = """
                INSERT INTO soracare.notificaciones (
                    id_persona, id_cita, id_pago, id_plantilla_notificacion,
                    id_canal_notificacion, id_estado_notificacion,
                    destinatario, asunto, contenido, clave_idempotencia,
                    fecha_envio, fecha_entrega, proveedor, referencia_proveedor
                )
                SELECT paciente.id_persona, cita.id_cita, ?,
                       plantilla.id_plantilla_notificacion,
                       canal.id_canal_notificacion, estado.id_estado_notificacion,
                       'PORTAL:' || paciente.id_persona,
                       'Pago confirmado', ?, ?,
                       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                       'SORACARE_INTERNO', ?
                  FROM soracare.citas cita
                  INNER JOIN soracare.pacientes paciente
                          ON paciente.id_paciente = cita.id_paciente
                  CROSS JOIN soracare.canales_notificacion canal
                  CROSS JOIN soracare.estados_notificacion estado
                  LEFT JOIN soracare.plantillas_notificacion plantilla
                         ON plantilla.codigo = 'PAGO_CONFIRMADO_SISTEMA'
                 WHERE cita.id_cita = ?
                   AND canal.codigo = 'SISTEMA'
                   AND estado.codigo = 'ENTREGADA'
                ON CONFLICT (clave_idempotencia) DO NOTHING
                """;
        String clave = "PAGO-" + idPago + "-SISTEMA-1";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPago);
            sentencia.setString(2, contenido);
            sentencia.setString(3, clave);
            sentencia.setString(4, numeroComprobante);
            sentencia.setLong(5, idCita);
            sentencia.executeUpdate();
        }
    }

    public void registrarFacturaInternaEmitida(
            Connection conexion,
            long idCita,
            long idPago,
            String numeroFactura
    ) throws SQLException {
        String sql = """
                INSERT INTO soracare.notificaciones (
                    id_persona, id_cita, id_pago,
                    id_canal_notificacion, id_estado_notificacion,
                    destinatario, asunto, contenido, clave_idempotencia,
                    fecha_envio, fecha_entrega, proveedor, referencia_proveedor
                )
                SELECT paciente.id_persona, cita.id_cita, ?,
                       canal.id_canal_notificacion, estado.id_estado_notificacion,
                       'PORTAL:' || paciente.id_persona,
                       'Factura interna disponible',
                       'La factura interna ' || ?
                           || ' ya está disponible en Mis citas. '
                           || 'Es un documento comercial interno sin validez tributaria.',
                       ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                       'SORACARE_INTERNO', ?
                  FROM soracare.citas cita
                  INNER JOIN soracare.pacientes paciente
                          ON paciente.id_paciente = cita.id_paciente
                  CROSS JOIN soracare.canales_notificacion canal
                  CROSS JOIN soracare.estados_notificacion estado
                 WHERE cita.id_cita = ?
                   AND canal.codigo = 'SISTEMA'
                   AND estado.codigo = 'ENTREGADA'
                ON CONFLICT (clave_idempotencia) DO NOTHING
                """;
        String clave = "FACTURA-INTERNA-" + idPago + "-SISTEMA-1";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPago);
            sentencia.setString(2, numeroFactura);
            sentencia.setString(3, clave);
            sentencia.setString(4, numeroFactura);
            sentencia.setLong(5, idCita);
            sentencia.executeUpdate();
        }
    }

    public void registrarRecetaEmitida(
            Connection conexion, RecetaMedicaDTO receta
    ) throws SQLException {
        String sql = """
                INSERT INTO soracare.notificaciones (
                    id_persona, id_receta, id_plantilla_notificacion,
                    id_canal_notificacion, id_estado_notificacion,
                    destinatario, asunto, contenido, clave_idempotencia,
                    fecha_envio, fecha_entrega, proveedor, referencia_proveedor
                )
                SELECT pac.id_persona, ?, plantilla.id_plantilla_notificacion,
                       canal.id_canal_notificacion, estado.id_estado_notificacion,
                       'PORTAL:' || pac.id_persona,
                       'Nueva indicación médica disponible',
                       'La indicación ' || ? || ' fue registrada por ' || ?
                           || '. Ya podés consultarla y descargarla desde Mis indicaciones médicas.',
                       ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                       'SORACARE_INTERNO', ?
                  FROM soracare.pacientes pac
                  CROSS JOIN soracare.canales_notificacion canal
                  CROSS JOIN soracare.estados_notificacion estado
                  LEFT JOIN soracare.plantillas_notificacion plantilla
                         ON plantilla.codigo = 'RECETA_EMITIDA_SISTEMA'
                 WHERE pac.id_paciente = ?
                   AND canal.codigo = 'SISTEMA'
                   AND estado.codigo = 'ENTREGADA'
                ON CONFLICT (clave_idempotencia) DO NOTHING
                """;
        String clave = "RECETA-" + receta.idReceta() + "-SISTEMA-1";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, receta.idReceta());
            sentencia.setString(2, receta.codigo());
            sentencia.setString(3, receta.nombreMedico());
            sentencia.setString(4, clave);
            sentencia.setString(5, receta.codigo());
            sentencia.setLong(6, receta.idPaciente());
            sentencia.executeUpdate();
        }
    }

    public List<NotificacionDTO> listarInternasPorPersona(
            Connection conexion, long idPersona
    ) throws SQLException {
        String sql = """
                SELECT noti.id_notificacion, noti.id_cita, noti.id_pago,
                       noti.id_receta, canal.codigo AS canal,
                       estado.codigo AS estado, noti.asunto, noti.contenido,
                       noti.fecha_creacion, noti.fecha_lectura
                  FROM soracare.notificaciones noti
                  INNER JOIN soracare.canales_notificacion canal
                          ON canal.id_canal_notificacion = noti.id_canal_notificacion
                  INNER JOIN soracare.estados_notificacion estado
                          ON estado.id_estado_notificacion = noti.id_estado_notificacion
                 WHERE noti.id_persona = ? AND canal.codigo = 'SISTEMA'
                 ORDER BY noti.fecha_creacion DESC, noti.id_notificacion DESC
                 LIMIT 100
                """;
        List<NotificacionDTO> notificaciones = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPersona);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    notificaciones.add(new NotificacionDTO(
                            resultado.getLong("id_notificacion"),
                            largoNulo(resultado, "id_cita"),
                            largoNulo(resultado, "id_pago"),
                            largoNulo(resultado, "id_receta"),
                            resultado.getString("canal"),
                            resultado.getString("estado"),
                            resultado.getString("asunto"),
                            resultado.getString("contenido"),
                            resultado.getObject("fecha_creacion", OffsetDateTime.class),
                            resultado.getObject("fecha_lectura", OffsetDateTime.class)
                    ));
                }
            }
        }
        return notificaciones;
    }

    public long contarInternasNoLeidas(Connection conexion, long idPersona)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                  FROM soracare.notificaciones noti
                  INNER JOIN soracare.canales_notificacion canal
                          ON canal.id_canal_notificacion = noti.id_canal_notificacion
                 WHERE noti.id_persona = ?
                   AND canal.codigo = 'SISTEMA'
                   AND noti.fecha_lectura IS NULL
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPersona);
            try (ResultSet resultado = sentencia.executeQuery()) {
                resultado.next();
                return resultado.getLong(1);
            }
        }
    }

    public void marcarLeida(
            Connection conexion, long idNotificacion, long idPersona
    ) throws SQLException {
        String sql = """
                UPDATE soracare.notificaciones
                   SET fecha_lectura = COALESCE(fecha_lectura, CURRENT_TIMESTAMP)
                 WHERE id_notificacion = ? AND id_persona = ?
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idNotificacion);
            sentencia.setLong(2, idPersona);
            if (sentencia.executeUpdate() != 1) {
                throw new IllegalArgumentException("La notificación seleccionada no existe.");
            }
        }
    }

    public void marcarTodasLeidas(Connection conexion, long idPersona) throws SQLException {
        String sql = """
                UPDATE soracare.notificaciones noti
                   SET fecha_lectura = CURRENT_TIMESTAMP
                  FROM soracare.canales_notificacion canal
                 WHERE noti.id_persona = ? AND noti.fecha_lectura IS NULL
                   AND canal.id_canal_notificacion = noti.id_canal_notificacion
                   AND canal.codigo = 'SISTEMA'
                """;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idPersona);
            sentencia.executeUpdate();
        }
    }

    private static Long largoNulo(ResultSet resultado, String columna) throws SQLException {
        long valor = resultado.getLong(columna);
        return resultado.wasNull() ? null : valor;
    }
}
