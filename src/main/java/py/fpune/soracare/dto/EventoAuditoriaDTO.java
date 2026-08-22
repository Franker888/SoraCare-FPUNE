package py.fpune.soracare.dto;

import java.time.OffsetDateTime;

public record EventoAuditoriaDTO(
        long idAuditoria,
        Long idUsuario,
        String actor,
        String nombreUsuario,
        String tipoEvento,
        String modulo,
        String entidad,
        String idRegistro,
        String descripcion,
        String direccionIp,
        String metodoHttp,
        String rutaSolicitud,
        String resultado,
        String mensajeError,
        OffsetDateTime fechaEvento
) {
}
