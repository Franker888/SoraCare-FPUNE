package py.fpune.soracare.dto;

import java.time.OffsetDateTime;

public record ConsultaSifenHistorialDTO(long idConsulta, long idSolicitud, String cdc,
        OffsetDateTime iniciadoEn, String estado, String codigo, String mensaje,
        String protocolo, Integer eventos, String observacionEventos) { }
