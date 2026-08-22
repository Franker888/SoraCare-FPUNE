package py.fpune.soracare.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InformeAdministrativoDTO(
        long totalCitas,
        long citasPeriodoAnterior,
        long citasConfirmadas,
        long citasAtendidas,
        long citasCanceladas,
        long citasReprogramadas,
        long busquedasTurnos,
        long pacientesRegistrados,
        BigDecimal ingresosConfirmados,
        List<PuntoFechaDTO> citasPorFecha,
        List<DistribucionDTO> citasPorEspecialidad,
        List<DistribucionDTO> citasPorMedico,
        List<DistribucionDTO> citasPorEstado,
        List<CeldaDemandaDTO> mapaCalor
) {
    public InformeAdministrativoDTO {
        ingresosConfirmados = ingresosConfirmados == null
                ? BigDecimal.ZERO : ingresosConfirmados;
        citasPorFecha = List.copyOf(citasPorFecha);
        citasPorEspecialidad = List.copyOf(citasPorEspecialidad);
        citasPorMedico = List.copyOf(citasPorMedico);
        citasPorEstado = List.copyOf(citasPorEstado);
        mapaCalor = List.copyOf(mapaCalor);
    }

    public static InformeAdministrativoDTO vacio() {
        return new InformeAdministrativoDTO(
                0, 0, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO,
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }

    public Double variacionCitasPorcentual() {
        if (citasPeriodoAnterior == 0) {
            return totalCitas == 0 ? 0D : null;
        }
        return ((double) totalCitas - citasPeriodoAnterior)
                * 100D / citasPeriodoAnterior;
    }

    public double tasaAtencion() {
        return totalCitas == 0 ? 0D : (double) citasAtendidas * 100D / totalCitas;
    }

    public long maximoSerie() {
        return citasPorFecha.stream().mapToLong(PuntoFechaDTO::cantidad).max().orElse(0);
    }

    public long maximoEspecialidades() {
        return citasPorEspecialidad.stream().mapToLong(DistribucionDTO::cantidad).max().orElse(0);
    }

    public long maximoMapaCalor() {
        return mapaCalor.stream().mapToLong(CeldaDemandaDTO::cantidad).max().orElse(0);
    }

    public record PuntoFechaDTO(LocalDate fecha, long cantidad) {
    }

    public record DistribucionDTO(String etiqueta, long cantidad) {
    }

    public record CeldaDemandaDTO(int diaSemana, int hora, long cantidad) {
    }
}
