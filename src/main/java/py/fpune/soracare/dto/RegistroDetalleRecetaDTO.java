package py.fpune.soracare.dto;

public record RegistroDetalleRecetaDTO(
        String idMedicamento,
        String dosis,
        String frecuencia,
        String duracionDias,
        String cantidad,
        String unidad,
        String instrucciones
) {
}
