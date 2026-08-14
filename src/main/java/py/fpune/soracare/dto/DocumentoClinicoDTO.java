package py.fpune.soracare.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record DocumentoClinicoDTO(
        long idDocumento,
        long idPaciente,
        Long idAtencion,
        short idTipoDocumento,
        String codigoTipo,
        String nombreTipo,
        String nombreOriginal,
        String tipoMime,
        long tamanoBytes,
        String descripcion,
        String origen,
        LocalDate fechaDocumento,
        OffsetDateTime fechaCreacion,
        String cargadoPor
) {
    public boolean esImagen() {
        return tipoMime != null && tipoMime.startsWith("image/");
    }

    public boolean esPdf() {
        return "application/pdf".equals(tipoMime);
    }
}
