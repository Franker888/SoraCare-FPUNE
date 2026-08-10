package py.fpune.soracare.dto;

public record SolicitudPagoDTO(
        String codigoMetodo,
        DatosFacturacionDTO datosFacturacion
) {
}
