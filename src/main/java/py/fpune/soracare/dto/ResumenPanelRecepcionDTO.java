package py.fpune.soracare.dto;

public record ResumenPanelRecepcionDTO(
        long citasHoy,
        long confirmadasHoy,
        long porConfirmarHoy,
        long cobrosPendientes
) {
}
