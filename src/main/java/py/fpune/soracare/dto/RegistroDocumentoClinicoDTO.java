package py.fpune.soracare.dto;

import java.time.LocalDate;

public record RegistroDocumentoClinicoDTO(
        long idPaciente,
        Long idAtencion,
        short idTipoDocumento,
        long idUsuarioCarga,
        String nombreAlmacenado,
        String nombreOriginal,
        String extension,
        String tipoMime,
        long tamanoBytes,
        String rutaAlmacenamiento,
        String hashSha256,
        String descripcion,
        LocalDate fechaDocumento
) {
}
