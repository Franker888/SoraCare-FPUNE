package py.fpune.soracare.dto;

public record FacturaInternaArchivoDTO(
        long idFactura,
        long idPago,
        long idPersonaPaciente,
        long idPersonaMedico,
        String numeroFactura,
        String nombreArchivo,
        byte[] contenidoPdf
) {
}
