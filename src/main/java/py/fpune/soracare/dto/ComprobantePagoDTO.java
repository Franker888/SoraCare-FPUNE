package py.fpune.soracare.dto;

public record ComprobantePagoDTO(
        long idComprobante,
        long idPago,
        long idPaciente,
        String nombreArchivo,
        byte[] contenidoPdf
) {
}
