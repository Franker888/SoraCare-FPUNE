package py.fpune.soracare.dto;

import java.time.LocalDate;

public record FiltroInformeDTO(
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        Long idMedico,
        Integer idEspecialidad
) {
    public int cantidadDias() {
        return (int) (fechaHasta.toEpochDay() - fechaDesde.toEpochDay()) + 1;
    }

    public FiltroInformeDTO periodoAnterior() {
        LocalDate finAnterior = fechaDesde.minusDays(1);
        return new FiltroInformeDTO(
                finAnterior.minusDays(cantidadDias() - 1L),
                finAnterior,
                idMedico,
                idEspecialidad
        );
    }
}
