package py.fpune.soracare.dto;

public record DocumentoSifenEnvioDTO(
        long idSolicitud,
        long idFacturaInterna,
        String ambiente,
        String estado,
        String cdc,
        byte[] xmlFirmado
) {
    public DocumentoSifenEnvioDTO {
        xmlFirmado = xmlFirmado == null ? new byte[0] : xmlFirmado.clone();
    }

    @Override
    public byte[] xmlFirmado() {
        return xmlFirmado.clone();
    }
}
