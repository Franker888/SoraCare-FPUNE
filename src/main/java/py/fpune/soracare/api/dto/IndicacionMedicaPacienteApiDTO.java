package py.fpune.soracare.api.dto;

import py.fpune.soracare.dto.RecetaMedicaDTO;

import java.util.List;

public record IndicacionMedicaPacienteApiDTO(
        long idIndicacion,
        String codigo,
        String fechaEmision,
        String fechaVencimiento,
        String indicacionesGenerales,
        String estado,
        String fechaAnulacion,
        String motivoAnulacion,
        String medico,
        String especialidad,
        List<DetalleIndicacionMedicaApiDTO> medicamentos
) {

    public IndicacionMedicaPacienteApiDTO {
        medicamentos = medicamentos == null
                ? List.of()
                : List.copyOf(medicamentos);
    }

    public static IndicacionMedicaPacienteApiDTO desde(
            RecetaMedicaDTO receta
    ) {
        return new IndicacionMedicaPacienteApiDTO(
                receta.idReceta(),
                receta.codigo(),
                texto(receta.fechaEmision()),
                texto(receta.fechaVencimiento()),
                receta.indicacionesGenerales(),
                receta.estado(),
                texto(receta.fechaAnulacion()),
                receta.motivoAnulacion(),
                receta.nombreMedico(),
                receta.especialidad(),
                receta.detalles().stream()
                        .map(DetalleIndicacionMedicaApiDTO::desde)
                        .toList()
        );
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }
}
