package py.fpune.soracare.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record RecetaMedicaDTO(
        long idReceta,
        long idAtencion,
        long idPaciente,
        long idMedico,
        String codigo,
        OffsetDateTime fechaEmision,
        LocalDate fechaVencimiento,
        String indicacionesGenerales,
        String estado,
        OffsetDateTime fechaAnulacion,
        String motivoAnulacion,
        String nombrePaciente,
        String nombreMedico,
        String especialidad,
        String emailIndicacionesDestino,
        String estadoEnvioIndicacionesEmail,
        OffsetDateTime fechaEnvioIndicacionesEmail,
        List<DetalleRecetaDTO> detalles
) {
    public RecetaMedicaDTO {
        detalles = detalles == null ? List.of() : List.copyOf(detalles);
    }

    public boolean estaEmitida() {
        return "EMITIDA".equals(estado);
    }

    public String textoEstadoEnvioIndicaciones() {
        if ("ENVIADO".equals(estadoEnvioIndicacionesEmail)) {
            return "Enviado por Resend";
        }
        if ("PENDIENTE".equals(estadoEnvioIndicacionesEmail)) {
            return "Envío en proceso";
        }
        if ("FALLIDO".equals(estadoEnvioIndicacionesEmail)) {
            return "No pudo enviarse";
        }
        if ("NO_CONFIGURADO".equals(estadoEnvioIndicacionesEmail)) {
            return "Correo no configurado";
        }
        return "Aún no enviado";
    }
}
