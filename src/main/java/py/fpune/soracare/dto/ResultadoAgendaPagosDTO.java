package py.fpune.soracare.dto;

import java.util.List;

public record ResultadoAgendaPagosDTO(
        List<PagoResumenDTO> pagos,
        long totalResultados,
        int paginaActual,
        int totalPaginas,
        int tamanoPagina,
        long pagosPendientes,
        long efectivoPendiente,
        long digitalPendiente
) {

    public ResultadoAgendaPagosDTO {
        pagos = pagos == null ? List.of() : List.copyOf(pagos);
    }

    public static ResultadoAgendaPagosDTO vacio(int tamanoPagina) {
        return new ResultadoAgendaPagosDTO(
                List.of(), 0, 1, 0, tamanoPagina, 0, 0, 0
        );
    }

    public boolean tienePaginaAnterior() {
        return paginaActual > 1;
    }

    public boolean tienePaginaSiguiente() {
        return paginaActual < totalPaginas;
    }
}
