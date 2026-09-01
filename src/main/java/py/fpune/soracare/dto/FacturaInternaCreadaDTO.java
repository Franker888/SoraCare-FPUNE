package py.fpune.soracare.dto;

public record FacturaInternaCreadaDTO(
        long idFactura,
        long idPago,
        String numeroFactura,
        String estadoEnvioEmail
) {
}
