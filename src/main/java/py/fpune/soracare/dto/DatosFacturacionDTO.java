package py.fpune.soracare.dto;

public record DatosFacturacionDTO(
        String tipoDocumento,
        String numeroDocumento,
        String nombreRazonSocial,
        String direccion,
        String correo
) {
}
