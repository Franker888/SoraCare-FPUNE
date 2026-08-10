package py.fpune.soracare.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PagoResumenDTO(
        long idPago,
        long idCita,
        long idPaciente,
        String codigoMetodo,
        String nombreMetodo,
        String codigoEstado,
        String nombreEstado,
        BigDecimal monto,
        String moneda,
        String referenciaExterna,
        OffsetDateTime fechaConfirmacion,
        OffsetDateTime fechaCita,
        String nombrePaciente,
        String nombreMedico,
        String nombreEspecialidad,
        String correoFacturacion,
        Long idComprobante,
        String numeroComprobante,
        String estadoEnvioEmail
) {

    public boolean estaConfirmado() {
        return "CONFIRMADO".equals(codigoEstado);
    }

    public boolean estaPendiente() {
        return "PENDIENTE".equals(codigoEstado);
    }

    public boolean esEfectivo() {
        return "EFECTIVO".equals(codigoMetodo);
    }

    public boolean esDigital() {
        return !esEfectivo();
    }
}
