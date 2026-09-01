package py.fpune.soracare.dto;

import java.time.OffsetDateTime;

public record SolicitudSifenResumenDTO(
        long idSolicitud,
        long idFacturaInterna,
        String ambiente,
        String estado,
        String numeroDocumento,
        String cdc,
        boolean xsdValidado,
        String codigoRespuesta,
        String mensajeRespuesta,
        String urlQr,
        OffsetDateTime actualizadoEn
) {
    public boolean aprobada() { return "APROBADO".equals(estado); }
    public boolean rechazada() { return "RECHAZADO".equals(estado); }
    public boolean validadaLocalmente() {
        return xsdValidado && ("VALIDADO".equals(estado)
                || "ENVIADO".equals(estado)
                || aprobada() || rechazada());
    }
}
