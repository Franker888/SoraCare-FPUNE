package py.fpune.soracare.dto;

public record TipoDocumentoClinicoDTO(
        short idTipoDocumento,
        String codigo,
        String nombre,
        String descripcion
) {
}
