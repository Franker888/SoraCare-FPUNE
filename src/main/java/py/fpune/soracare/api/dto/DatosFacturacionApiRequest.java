package py.fpune.soracare.api.dto;

import py.fpune.soracare.dto.DatosFacturacionDTO;

public record DatosFacturacionApiRequest(
        String tipoDocumento,
        String numeroDocumento,
        String nombreRazonSocial,
        String direccion,
        String correo
) {

    public DatosFacturacionDTO aSolicitudInterna() {
        return new DatosFacturacionDTO(
                tipoDocumento,
                numeroDocumento,
                nombreRazonSocial,
                direccion,
                correo
        );
    }
}
