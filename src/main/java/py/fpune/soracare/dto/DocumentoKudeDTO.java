package py.fpune.soracare.dto;

public record DocumentoKudeDTO(String ambiente, String estado, String cdc,
                               byte[] xmlFirmado, String sha256, boolean xsdValidado) {
    public DocumentoKudeDTO {
        xmlFirmado = xmlFirmado == null ? new byte[0] : xmlFirmado.clone();
    }

    @Override
    public byte[] xmlFirmado() { return xmlFirmado.clone(); }
}
