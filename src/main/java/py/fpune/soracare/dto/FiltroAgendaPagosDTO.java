package py.fpune.soracare.dto;

import java.time.LocalDate;

public record FiltroAgendaPagosDTO(
        String cedula,
        String metodo,
        String estado,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        int pagina,
        int tamanoPagina
) {

    public FiltroAgendaPagosDTO {
        cedula = cedula == null ? "" : cedula;
        metodo = metodo == null ? "" : metodo;
        estado = estado == null ? "" : estado;
        if (pagina < 1) pagina = 1;
        if (tamanoPagina < 1) tamanoPagina = 20;
    }

    public static FiltroAgendaPagosDTO vacio(int tamanoPagina) {
        return new FiltroAgendaPagosDTO(
                "", "", "", null, null, 1, tamanoPagina
        );
    }

    public int desplazamiento() {
        return (pagina - 1) * tamanoPagina;
    }
}
