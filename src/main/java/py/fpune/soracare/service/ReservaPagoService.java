package py.fpune.soracare.service;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.dto.PagoResumenDTO;
import py.fpune.soracare.dto.ResultadoReservaDTO;
import py.fpune.soracare.dto.SolicitudCitaDTO;
import py.fpune.soracare.dto.SolicitudPagoDTO;
import py.fpune.soracare.mvc.model.Cita;

import java.sql.Connection;
import java.sql.SQLException;

public class ReservaPagoService {

    private final CitaService citaService = new CitaService();
    private final PagoService pagoService = new PagoService();

    public ResultadoReservaDTO reservar(
            SolicitudCitaDTO solicitudCita,
            SolicitudPagoDTO solicitudPago,
            long idUsuario
    ) throws SQLException {
        if (idUsuario <= 0) throw new IllegalArgumentException("El usuario autenticado no es valido.");
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            try {
                conexion.setAutoCommit(false);
                Cita cita = citaService.solicitar(conexion, solicitudCita, idUsuario);
                PagoResumenDTO pago = pagoService.crearPendiente(
                        conexion, cita, solicitudPago, idUsuario);
                conexion.commit();
                return new ResultadoReservaDTO(cita, pago);
            } catch (Exception error) {
                rollback(conexion, error);
                if (error instanceof IllegalArgumentException validacion) throw validacion;
                if (error instanceof SQLException sql) throw sql;
                throw new SQLException("No fue posible reservar la cita con su pago.", error);
            }
        }
    }

    private static void rollback(Connection conexion, Exception original) {
        try {
            conexion.rollback();
        } catch (SQLException rollback) {
            original.addSuppressed(rollback);
        }
    }
}
