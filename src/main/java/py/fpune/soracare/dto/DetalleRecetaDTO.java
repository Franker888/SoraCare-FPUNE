package py.fpune.soracare.dto;

import py.fpune.soracare.mvc.model.ViaAdministracion;

import java.math.BigDecimal;

public record DetalleRecetaDTO(
        long idDetalleReceta,
        long idMedicamento,
        String nombreComercial,
        String principioActivo,
        String presentacion,
        String concentracion,
        ViaAdministracion viaAdministracion,
        String dosis,
        String frecuencia,
        Integer duracionDias,
        BigDecimal cantidad,
        String unidad,
        String instrucciones
) {
}
