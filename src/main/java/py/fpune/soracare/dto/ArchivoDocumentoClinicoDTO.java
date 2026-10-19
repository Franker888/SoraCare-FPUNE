package py.fpune.soracare.dto;

public record ArchivoDocumentoClinicoDTO(
        long idDocumento,
        long idPaciente,
        String nombreOriginal,
        String tipoMime,
        long tamanoBytes,
        String rutaAlmacenamiento,
        byte[] contenido,
        String hashSha256
) {
}
